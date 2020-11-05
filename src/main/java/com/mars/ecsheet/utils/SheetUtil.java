package com.mars.ecsheet.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
public class SheetUtil {


    /**
     * 获取sheet的默认option
     *
     * @return
     */
    public static JSONObject getDefautOption() {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("container", "ecsheet");
        jsonObject.put("title", "ecsheet demo");
        jsonObject.put("lang", "zh");
        jsonObject.put("allowUpdate", true);
        jsonObject.put("loadUrl", "");
        jsonObject.put("loadSheetUrl", "");
        jsonObject.put("updateUrl", "");

        return jsonObject;
    }

    /**
     * 获取默认的sheetData
     *
     * @return
     */
    public static List<JSONObject> getDefaultSheetData() {
        List<JSONObject> list = new ArrayList<>();

        for (int i = 1; i < 4; i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("row", 84);
            jsonObject.put("column", 60);
            jsonObject.put("name", "sheet" + i);
            Integer index = i - 1;
            jsonObject.put("index", IdUtil.simpleUUID());
            jsonObject.put("order", i - 1);
            if (i == 1) {
                jsonObject.put("status", 1);
            } else {
                jsonObject.put("status", 0);
            }
            jsonObject.put("celldata", new ArrayList<JSONObject>() {
            });
            list.add(jsonObject);
        }
        return list;
    }

    /**
     * 获取默认的全部sheetData
     *
     * @return
     */
    public static JSONObject getDefaultAllSheetData() {
        JSONObject result = new JSONObject();

        for (int i = 1; i < 4; i++) {
            JSONObject data = new JSONObject();
            data.put("r", 0);
            data.put("c", 0);
            data.put("v", new JSONObject());
            result.put("sheet" + i, data);
        }
        return result;
    }

    /**
     * 组装异步加载sheet所需的数据
     *
     * @param data
     * @return
     */
    public static JSONObject buildSheetData(List<JSONObject> data) {
        JSONObject result = new JSONObject();
        data.forEach((d) -> {
            result.put(d.get("index").toString(), d.get("celldata"));
        });

        return result;
    }


}
