package com.mars.ecsheet.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mars.ecsheet.entity.WorkBookEntity;
import com.mars.ecsheet.entity.WorkSheetEntity;
import com.mars.ecsheet.repository.WorkBookRepository;
import com.mars.ecsheet.repository.WorkSheetRepository;
import com.mars.ecsheet.service.IMessageProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
@Service
public class MessageProcess implements IMessageProcess {


    @Autowired
    private WorkSheetRepository workSheetRepository;

    @Autowired
    private WorkBookRepository workBookRepository;

    @Override
    public void process(String wbId, JSONObject message) {
        //获取操作名
        String action = message.getStr("t");
        //获取sheet的index值
        String index = message.getStr("i");

        //如果是复制sheet，index的值需要另取
        if ("shc".equals(action)) {
            index = message.getJSONObject("v").getStr("copyindex");
        }

        //如果是删除sheet，index的值需要另取
        if ("shd".equals(action)) {
            index = message.getJSONObject("v").getStr("deleIndex");
        }

        //如果是恢复sheet，index的值需要另取
        if ("shre".equals(action)) {
            index = message.getJSONObject("v").getStr("reIndex");
        }

        WorkSheetEntity ws = workSheetRepository.findByindexAndwbId(index, wbId);

        switch (action) {
            //单个单元格刷新
            case "v":
                ws = singleCellRefresh(ws, message);
                break;
            //范围单元格刷新
            case "rv":
                ws = rangeCellRefresh(ws, message);
                break;
            //config操作
            case "cg":
                ws = configRefresh(ws, message);
                break;
            //通用保存
            case "all":
                ws = allRefresh(ws, message);
                break;
            //函数链操作
            case "fc":
                ws = calcChainRefresh(ws, message);
                break;
            //删除行或列
            case "drc":
                ws = drcRefresh(ws, message);
                break;
            //增加行或列
            case "arc":
                ws = arcRefresh(ws, message);
                break;
            //清除筛选
            case "fsc":
                ws = fscRefresh(ws, message);
                break;
            //恢复筛选
            case "fsr":
                ws = fscRefresh(ws, message);
                break;
            //新建sheet
            case "sha":
                ws = shaRefresh(wbId, message);
                break;
            //切换到指定sheet
            case "shs":
                shsRefresh(wbId, message);
                break;
            //复制sheet
            case "shc":
                ws = shcRefresh(ws, message);
                break;
            //修改工作簿名称
            case "na":
                naRefresh(wbId, message);
                break;
            //删除sheet
            case "shd":
                ws.setDeleteStatus(1);
                break;
            //删除sheet后恢复操作
            case "shre":
                ws.setDeleteStatus(0);
                break;
            //调整sheet位置
            case "shr":
                shrRefresh(wbId, message);
                break;
            //sheet属性(隐藏或显示)
            case "sh":
                ws = shRefresh(ws, message);
                break;
            default:
                break;
        }
        if (ObjectUtil.isNull(ws)) {
            return;
        }
        workSheetRepository.save(ws);

    }

    /**
     * 单个单元格刷新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity singleCellRefresh(WorkSheetEntity ws, JSONObject message) {
        //对celldata进行深拷贝
        JSONArray celldata = ObjectUtil.cloneByStream(ws.getData().getJSONArray("celldata"));
        if (StrUtil.isBlank(message.getStr("v"))) {
            celldata.forEach(c -> {
                JSONObject jsonObject = JSONUtil.parseObj(c);
                if (!jsonObject.isEmpty()) {
                    if (jsonObject.getLong("r") == message.getLong("r") && jsonObject.getLong("c") == message.getLong("c")) {
                        ws.getData().getJSONArray("celldata").remove(jsonObject);
                    }
                }
            });
        } else {
            JSONObject collectData = JSONUtil.createObj().put("r", message.getLong("r")).put("c", message.getLong("c")).put("v", message.getJSONObject("v"));

            List<String> flag = new ArrayList<>();
            celldata.forEach(c -> {
                JSONObject jsonObject = JSONUtil.parseObj(c);
                if (!jsonObject.isEmpty()) {
                    if (jsonObject.getLong("r") == message.getLong("r") && jsonObject.getLong("c") == message.getLong("c")) {
                        ws.getData().getJSONArray("celldata").remove(jsonObject);
                        ws.getData().getJSONArray("celldata").add(collectData);
                        flag.add("used");
                    }
                }
            });
            if (flag.isEmpty()) {
                ws.getData().getJSONArray("celldata").add(collectData);
            }
        }
        return ws;
    }


    /**
     * 范围单元格刷新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity rangeCellRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray rowArray = message.getJSONObject("range").getJSONArray("row");
        JSONArray columnArray = message.getJSONObject("range").getJSONArray("column");
        JSONArray vArray = message.getJSONArray("v");
        JSONArray celldata = ObjectUtil.cloneByStream(ws.getData().getJSONArray("celldata"));
        int countRowIndex = 0;

        //遍历行列，对符合行列的内容进行更新
        for (int ri = (int) rowArray.get(0); ri <= (int) rowArray.get(1); ri++) {
            int countColumnIndex = 0;
            for (int ci = (int) columnArray.get(0); ci <= (int) columnArray.get(1); ci++) {
                List<String> flag = new ArrayList<>();
                Object newCell = JSONUtil.parseArray(vArray.get(countRowIndex)).get(countColumnIndex);
                JSONObject collectData = JSONUtil.createObj().put("r", ri).put("c", ci).put("v", newCell);
                int rowIndex = ri;
                int columnIndex = ci;
                celldata.forEach(cell -> {
                    JSONObject jsonObject = JSONUtil.parseObj(cell);

                    if (!jsonObject.isEmpty()) {
                        if (jsonObject.getInt("r") == rowIndex && jsonObject.getInt("c") == columnIndex) {
                            if ("null".equals(newCell.toString()) || JSONUtil.parseObj(newCell).isEmpty()) {
                                ws.getData().getJSONArray("celldata").remove(jsonObject);
                            } else {
                                ws.getData().getJSONArray("celldata").remove(jsonObject);
                                ws.getData().getJSONArray("celldata").add(collectData);

                            }
                            flag.add("used");
                        }
                    }
                });
                if (flag.isEmpty() && !JSONUtil.parseObj(newCell).isEmpty()) {
                    ws.getData().getJSONArray("celldata").add(collectData);
                }
                countColumnIndex++;
            }
            countRowIndex++;
        }


        return ws;
    }


    /**
     * config更新
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity configRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONObject v = message.getJSONObject("v");
        JSONObject newConfig = JSONUtil.createObj().put(message.getStr("k"), v);
        if (ws.getData().getJSONObject("config").isEmpty()) {
            ws.getData().put("config", newConfig);
        } else {
            ws.getData().getJSONObject("config").put(message.getStr("k"), v);
        }

        return ws;
    }


    /**
     * 通用保存
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity allRefresh(WorkSheetEntity ws, JSONObject message) {
        if (message.getJSONObject("v").isEmpty()) {
            ws.getData().remove(message.getStr("k"));
        } else {
            ws.getData().put(message.getStr("k"), message.getJSONObject("v"));
        }
        return ws;
    }


    /**
     * 函数链操作
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity calcChainRefresh(WorkSheetEntity ws, JSONObject message) {
        String value = message.getJSONObject("v").toString();
        if (!ws.getData().containsKey("calcChain")) {
            ws.getData().put("calcChain", new JSONArray());
        }
        JSONArray calcChain = ws.getData().getJSONArray("calcChain");
        if ("add".equals(message.getStr("op"))) {
            calcChain.add(value);
        } else if ("update".equals(message.getStr("op"))) {
            calcChain.remove(calcChain.get(message.getInt(message.getStr("pos"))));
            calcChain.add(value);
        } else if ("del".equals(message.getStr("op"))) {
            calcChain.remove(calcChain.get(message.getInt(message.getStr("pos"))));
        }
        return ws;
    }


    /**
     * 删除行或列
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity drcRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray celldata = ObjectUtil.cloneByStream(ws.getData().getJSONArray("celldata"));
        int index = message.getJSONObject("v").getInt("index");
        int len = message.getJSONObject("v").getInt("len");
        if ("r".equals(message.getStr("rc"))) {
            ws.getData().put("row", ws.getData().getInt("row") - len);
        } else {
            ws.getData().put("column", ws.getData().getInt("column") - len);
        }
        for (Object cell : celldata) {
            JSONObject jsonObject = JSONUtil.parseObj(cell);
            if ("r".equals(message.getStr("rc"))) {
                //删除行所在区域的内容
                if (jsonObject.getInt("r") >= index && jsonObject.getInt("r") < index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                }
                //增加大于 最大删除行的的行号
                if (jsonObject.getInt("r") >= index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInt("r") - len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
            } else {
                //删除列所在区域的内容
                if (jsonObject.getInt("c") >= index && jsonObject.getInt("c") < index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                }
                //增加大于 最大删除列的的列号
                if (jsonObject.getInt("c") >= index + len) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInt("c") - len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
            }
        }

        return ws;
    }


    /**
     * 增加行或列,暂未实现插入数据的情况
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity arcRefresh(WorkSheetEntity ws, JSONObject message) {
        JSONArray celldata = ObjectUtil.cloneByStream(ws.getData().getJSONArray("celldata"));
        int index = message.getJSONObject("v").getInt("index");
        int len = message.getJSONObject("v").getInt("len");

        for (Object cell : celldata) {
            JSONObject jsonObject = JSONUtil.parseObj(cell);
            if ("r".equals(message.getStr("rc"))) {
                //如果是增加行，且是向左增加
                if (jsonObject.getInt("r") >= index && "lefttop".equals(message.getJSONObject("v").getStr("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInt("r") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
                //如果是增加行，且是向右增加
                if (jsonObject.getInt("r") > index && "rightbottom".equals(message.getJSONObject("v").getStr("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("r", jsonObject.getInt("r") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }


            } else {
                //如果是增加列，且是向上增加
                if (jsonObject.getInt("c") >= index && "lefttop".equals(message.getJSONObject("v").getStr("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInt("c") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }
                //如果是增加列，且是向下增加
                if (jsonObject.getInt("c") > index && "rightbottom".equals(message.getJSONObject("v").getStr("direction"))) {
                    ws.getData().getJSONArray("celldata").remove(jsonObject);
                    jsonObject.put("c", jsonObject.getInt("c") + len);
                    ws.getData().getJSONArray("celldata").add(jsonObject);
                }

            }
        }
        JSONArray vArray = message.getJSONObject("v").getJSONArray("data");
        if ("r".equals(message.getStr("rc"))) {
            ws.getData().put("row", ws.getData().getInt("row") + len);
            for (int r = 0; r < vArray.size(); r++) {
                for (int c = 0; c < JSONUtil.parseArray(vArray.get(0)).size(); c++) {
                    if (JSONUtil.parseArray(vArray.get(r)).get(c) == null) {
                        continue;
                    }
                    JSONObject newCell = JSONUtil.createObj().put("r", r + index).put("c", c).put("v", JSONUtil.parseArray(vArray.get(r)).get(c));
                    ws.getData().getJSONArray("celldata").add(newCell);
                }
            }

        } else {
            ws.getData().put("column", ws.getData().getInt("column") + len);
            for (int r = 0; r < vArray.size(); r++) {
                for (int c = 0; c < JSONUtil.parseArray(vArray.get(0)).size(); c++) {
                    if (JSONUtil.parseArray(vArray.get(r)).get(c) == null) {
                        continue;
                    }
                    JSONObject newCell = JSONUtil.createObj().put("r", r).put("c", c + index).put("v", JSONUtil.parseArray(vArray.get(r)).get(c));
                    ws.getData().getJSONArray("celldata").add(newCell);
                }
            }
        }


        return ws;
    }


    /**
     * 筛选操作
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity fscRefresh(WorkSheetEntity ws, JSONObject message) {

        if (message.getJSONObject("v").isEmpty()) {
            ws.getData().remove("filter");
            ws.getData().remove("filter_select");
        } else {
            ws.getData().put("filter", message.getJSONObject("v").getJSONArray("filter"));
            ws.getData().put("filter_select", message.getJSONObject("v").getJSONObject("filter_select"));
        }
        return ws;
    }


    /**
     * 新建sheet
     *
     * @param wbId
     * @param message
     * @return
     */
    private WorkSheetEntity shaRefresh(String wbId, JSONObject message) {
        WorkSheetEntity ws = new WorkSheetEntity();
        ws.setWbId(wbId);
        ws.setId(IdUtil.simpleUUID());
        ws.setData(message.getJSONObject("v"));
        return ws;
    }


    /**
     * 复制sheet
     *
     * @param ws
     * @param message
     * @return
     */
    private WorkSheetEntity shcRefresh(WorkSheetEntity ws, JSONObject message) {

        String index = message.getStr("i");
        ws.setId(IdUtil.simpleUUID());
        ws.getData().put("index", index);
        ws.getData().put("name", message.getJSONObject("v").getStr("name"));

        return ws;
    }

    /**
     * 调整sheet位置
     *
     * @param wbId
     * @param message
     */
    private void shrRefresh(String wbId, JSONObject message) {
        List<WorkSheetEntity> allSheets = workSheetRepository.findAllBywbId(wbId);

        allSheets.forEach(sheet -> {
            sheet.getData().put("order", message.getJSONObject("v").getInt(sheet.getData().getStr("index")));
            workSheetRepository.save(sheet);
        });

    }

    /**
     * 切换到指定sheet
     *
     * @param ws
     * @param message
     * @return
     */
    private void shsRefresh(String wbId, JSONObject message) {
        WorkSheetEntity lastWs = workSheetRepository.findBystatusAndwbId(1, wbId);
        lastWs.getData().put("status", 0);
        WorkSheetEntity thisWs = workSheetRepository.findByindexAndwbId(message.getStr("v"), wbId);
        thisWs.getData().put("status", 1);
        workSheetRepository.save(lastWs);
        workSheetRepository.save(thisWs);
    }


    /**
     * sheet属性(隐藏或显示)
     *
     * @param wbId
     * @param message
     */
    private WorkSheetEntity shRefresh(WorkSheetEntity ws, JSONObject message) {
        Integer hideStatus = message.getInt("v");
        ws.getData().put("hide", hideStatus);

        WorkSheetEntity curWs = new WorkSheetEntity();

        if ("hide".equals(message.getStr("op"))) {
            ws.getData().put("status", 0);
            String cur = message.getStr("cur");
            curWs = workSheetRepository.findByindexAndwbId(cur, ws.getWbId());
            curWs.getData().put("status", 1);

        } else {
            curWs = workSheetRepository.findBystatusAndwbId(1, ws.getWbId());
            curWs.getData().put("status", 0);
        }

        workSheetRepository.save(curWs);
        return ws;
    }

    /**
     * 修改工作簿名称
     *
     * @param wbId
     * @param message
     * @return
     */
    private void naRefresh(String wbId, JSONObject message) {
        Optional<WorkBookEntity> wb = workBookRepository.findById(wbId);
        if (wb.isPresent()) {
            WorkBookEntity workBookEntity = wb.get();
            workBookEntity.getOption().put("title", message.getStr("v"));
            workBookRepository.save(workBookEntity);
        }
    }


}
