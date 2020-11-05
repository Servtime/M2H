package marray.top.mhtml.parase;

import lombok.extern.slf4j.Slf4j;
import marray.top.mhtml.Enum.IMhtmlPartEnum;
import marray.top.mhtml.Enum.IMhtmlScretEnum;
import marray.top.mhtml.constant.IMhtmlConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：feiyu
 * @date ：Created in 2020/11/4 15:34
 * @description：解析mhtml格式或者mht文件的工具类
 * @modified By：
 * @version: 1.1$
 */
@Slf4j
public class IMhtmlParas implements IMhtmlConstants {


    /**
     * 原文件 mhtml或者mht
     */
    private String mhtFile;
    /**
     * 正则表达式
     */
    private static final Pattern p = Pattern.compile("(\\w|_|-)+\\.\\w+");

    /**
     * boundary分隔符
     */
    private static final Pattern boundary = Pattern.compile(BOUNDARY_REGEX);

    /**
     * Content-Transfer-Encoding头部正则
     */
    private static final Pattern contentTransferEncodingPattern = Pattern.compile(CONTENT_TRANSFER_ENCODING_REGEX);

    /**
     * Content-Location正则
     */
    private static final Pattern contentLocation = Pattern.compile(CONTENT_LOCATION_REGEX);

    /**
     * 验证Content-Location|Content-ID是否存在的正则工具，并通过Content-Location|Content-ID去决定是否是属于
     * mhtml的内容
     */
    private static final Pattern partSign = Pattern.compile(PART_SIGN);


    /**
     * CONTENT_ID对应的正则表达式
     */
    private static final Pattern contentId = Pattern.compile(CONTENT_ID);
    /**
     * Content-Type:image/*
     */
    private static final Pattern contentTypeImage = Pattern.compile(IMAGE_REGEX);

    private static final Pattern contentType = Pattern.compile(CONTENT_TYPE);


    /**
     * 经过处理后的各部分
     */
    public List<String> boundaryParts = new ArrayList<String>(0);

    public boolean isinit = false;

    /**
     * 构建解析
     *
     * @param mhtFile
     */
    public IMhtmlParas(String mhtFile) {
        this.mhtFile = mhtFile;
    }

    /**
     * 解析mhtml模板数据,获取各部分的数据集合
     *
     * @return
     * @throws Exception
     */
    public IMhtmlParas init() {
        //将整个流转换成字符窜
        String content = this.mhtFile;
        //获取文本分隔符
        String boundary = getBoundary(content);
        if (boundary == null) {
            throw new RuntimeException("非规范的mhtml文件,缺少分隔符内容");
        }
        log.debug("mhtml的分隔符为:{}", boundary);
        isinit = true;
        String[] parts = content.split(boundary);
        List<String> partsAll = new ArrayList<String>(0);
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                log.warn("  {}  是文件头部,将被忽略", parts[i]);
                continue;
            }
            String part = parts[i];
            if (isPart(part)) {
                partsAll.add(part);
            } else {
                log.warn(" {}  不符合mhtml的内容规范，将忽略", part);
            }

        }
        boundaryParts.addAll(partsAll);
        return this;
    }

    /**
     * Content-ID: <frame-14866260E32CADC5D5ECDBF1C89D8ECE@mhtml.blink>
     * 是否包含part必须的元素即：Content-Location、Content-ID
     *
     * @param part
     * @return
     */
    private boolean isPart(String part) {
        boolean b = false;
        try {
            Matcher matcher = partSign.matcher(part);
            //防止遗留
            while (matcher.find()) {

                b = true;
            }
        } catch (Throwable e) {
            log.error("{}进行匹配异常，异常为{}", part, e);
        }
        return b;

    }


    /**
     * @param iMhtmlPartEnum 获取元素的
     * @return
     */
    public List<String> getParts(IMhtmlPartEnum iMhtmlPartEnum) {

        if (!isinit) {
            throw new RuntimeException("获取异常，请先初始化数据");
        }
        List<String> list = new ArrayList<String>(0);
        switch (iMhtmlPartEnum) {
            case CSS:
                list.addAll(findType(CSS_REGEX));
                break;
            case GIF:
            case PNG:
            case JPEG:
                list.addAll(findType(IMAGE_REGEX));
                break;
            case HTML:
                list.addAll(findType(HTML_REGEX));
                break;
            default:
                log.warn("无知类型");
                break;
        }

        return list;

    }

    private List<String> findType(String partsRegex) {

        if (ObjectUtils.isEmpty(boundaryParts)) {
            throw new RuntimeException("非规范的mhtml文件,未发现符合条件的内容项");
        }
        List<String> list = new ArrayList<String>(0);
        for (String boundaryPart : boundaryParts) {

            Pattern pattern = Pattern.compile(partsRegex);

            Matcher matcher = pattern.matcher(boundaryPart);

            if (matcher.find()) {
                list.add(reductionContent(boundaryPart));
            }

        }

        return list;

    }

    /**
     * 还原内容==》
     *
     * @param group 文本内容
     * @return
     */
    private String reductionContent(String group) {

        //首先获取png的还原
        String result = doDealContentTypePng(group);

        if (StringUtils.isBlank(result)) {
            result = doDealCssOrHtml(group);
        }

        return result;

    }

    /**
     * 处理content-type为text/html或text/css
     *
     * @param group 全部内容
     * @return 原本内容
     */
    private String doDealCssOrHtml(String group) {


        //获取编码格式
        String content = getContentTransferEncoding(group);

        String part = getPartContent(group);

        //加密类
        Class screat = IMhtmlScretEnum.getScretInstance(content);

        //加密方法
        String method = IMhtmlScretEnum.getMetodName(content);

        try {
            Object ob = MethodUtils.invokeStaticMethod(screat, method, part.getBytes());

            if (ob.getClass().isArray()) {
                return new String((byte[]) ob);
            } else if (ob instanceof String) {
                return ob.toString();
            } else {
                throw new RuntimeException("加密类型返回异常");
            }
        } catch (Exception e) {
            log.error("加密异常", e);
            throw new RuntimeException("执行加密返回异常");
        }

    }

    private String getContentType(String group) throws Exception {

        Matcher matcher = contentType.matcher(group);

        String content = null;
        while (matcher.find()) {
            if (StringUtils.isBlank(content)) {
                content = matcher.group();
            } else {
                throw new RuntimeException(String.format("%s不是规范的mhtml内容，因为包含多个Content-Type:xxxx/xxx"));
            }

        }
        if (StringUtils.isBlank(content)) {
            throw new Exception(String.format("%s不是规范的mhtml内容，因为包含多个Content-Type:xxxx/xxx"));
        }

        return content.substring(content.indexOf(":") + 1);
    }


    /**
     * 获取image/*的内容
     *
     * @param group
     * @return
     */
    private String doDealContentTypePng(String group) {

        String[] content = group.split(IMAGE_REGEX);
        if (content.length == 1) {
            log.warn("{}不是image/*类型", group);
        } else if (content.length != 2) {
            log.warn("{}不是规范的image/*内容，因为包含多个image/*");

        } else {
            try {
                String contentType = getContentType(group);
                String bm = getContentTransferEncoding(group);
                Class screat = IMhtmlScretEnum.getScretInstance(bm);
                if (screat.equals(Base64.class)) {
                    String image = String.format("data:%s;base64,", contentType, getPartContent(group));
                    return image;
                } else {
                    throw new RuntimeException(String.format("%s 不是base64内容，请介入处理", group));
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("%s 不是规范的image/*内容", group));
            }

        }

        return null;
    }

    /**
     * 获取mhtml的片段对应的全内容，不包含头信息
     *
     * @param group
     * @return
     */
    private String getPartContent(String group) {
        //首先使用location分隔
        String[] result = group.split(CONTENT_LOCATION_REGEX);
        //根据Content-Transfer-Encoding
        if (result.length == 1) {
            result = group.split(CONTENT_TRANSFER_ENCODING_REGEX);
        }
        if (result.length == 1) {
            throw new RuntimeException(String.format("%s 为非规范的mhtml文件,请手动添加提取纯内容的条件", group));
        }

        //获取location下的内容
        return result[1];
    }

    /**
     * 获取local提on对应的值
     *
     * @param group
     * @return
     */
    private String getLocation(String group) throws Exception {

        Matcher matcher = contentLocation.matcher(group);

        String content = null;
        while (matcher.find()) {
            if (StringUtils.isBlank(content)) {
                content = matcher.group();
            } else {
                throw new RuntimeException("非规范的mhtml文件,发现多个符合条件的Content-Location");
            }
        }
        if (StringUtils.isBlank(content)) {
            throw new Exception("非规范的mhtml文件,未发现符合条件的Content-Location");
        }

        int i = content.indexOf(":");

        return content.substring(i + 1);


    }

    /**
     * 获取Content-Transfer-Encoding对应的编码内容
     *
     * @param line
     * @return
     */
    private String getContentTransferEncoding(String line) {

        Matcher matcher = contentTransferEncodingPattern.matcher(line);
        String content = null;
        while (matcher.find()) {
            if (StringUtils.isBlank(content)) {
                content = matcher.group();
            } else {
                throw new RuntimeException(String.format("%s 非规范的mhtml文件,发现多个符合条件的Content-Transfer-Encoding", line));
            }
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException(String.format("%s非规范的mhtml文件,未发现符合条件的Content-Transfer-Encoding", line));
        }

        return content.split("\\:")[1];
    }

    /**
     * 从文档里提取 【电子邮件标头】里的文本分隔符
     */
    private String getBoundary(String content) {

        Matcher matcher = boundary.matcher(content);
        String result = null;
        while (matcher.find()) {
            if (result == null) {
                result = matcher.group();
            } else {
                throw new RuntimeException("非规范的mhtml文件,发现多个分隔符内容");
            }
        }

        result = result.split("\"")[1];
        return result;

    }


    /**
     * 将内容转换成html
     *
     * @return
     */
    public String convet2Html() {

        //校验是否初始化
        if (!isinit) {
            throw new RuntimeException("获取异常，请先初始化数据");
        }
        //转换数据==对各部分进行处理，并生成
        List<String> copyBoundaryParts = new ArrayList<String>(boundaryParts);
        Map<String, String> partMap = convertBoundary2Map(copyBoundaryParts);
        //
        Set<Map.Entry<String, String>> entrySet = partMap.entrySet();
        //先处理css、图片等资源
        for (Map.Entry<String, String> entry : entrySet) {
            String value = entry.getValue();
            for (Map.Entry<String, String> entry1 : entrySet) {
                if (value.contains(entry1.getKey())) {
                    //先处理css
                    if (StringUtils.deleteWhitespace(entry1.getKey()).startsWith("cid:") && entry1.getKey().contains("fram")
                            && value.contains(entry1.getKey())) {
                        //css处理
                        StringBuilder all = new StringBuilder();
                        String key = entry1.getValue();
                        StringBuilder builder = new StringBuilder("<style type=\"text/css\">");
                        builder.append(key);
                        builder.append("</style>");
                        StringBuilder builder1 = new StringBuilder();
                        all.append(value.substring(0, value.indexOf(entry1.getKey())));
                        String re = value.substring(value.indexOf(entry1.getKey()));
                        int i = re.indexOf(">");
                        builder1.append(re);
                        builder1.insert(i + 1, builder);
                        all.append(builder1);
                        partMap.remove(entry1.getKey());
                        partMap.put(entry.getKey(), all.toString());
                    }

                }
            }

        }

        //在处理html
        for (Map.Entry<String, String> entry : entrySet) {
            String value = entry.getValue();
            for (Map.Entry<String, String> entry1 : entrySet) {
                if (value.contains(entry1.getKey())) {
                    if (StringUtils.deleteWhitespace(entry1.getKey()).startsWith("frame")) {
                        String mValue = value.substring(value.indexOf(entry1.getKey()));
                        int index = mValue.indexOf("</iframe>");
                        StringBuilder newStr = new StringBuilder(mValue);
                        newStr.insert(index, entry1.getValue());
                        partMap.remove(entry1.getKey());
                        partMap.put(entry.getKey(), newStr.toString());
                    }

                }
            }

        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String p : partMap.values()) {

            if (p.contains("<!DOCTYPE html>")) {
                stringBuilder.append(p);
            }
        }

        return stringBuilder.toString();

    }

    /**
     * 将mhtml的各部分文件
     * 转换成key为Content-ID，value为其内容的值,只有该值才是会被放入html里的
     * 各部分的内容，首先会被检测contentID，然后才会被检测Content-Location
     *
     * @param copyBoundaryParts mhtml的内容集合
     * @return key为Content-ID、Content-Location，value为内容的map
     */
    private Map<String, String> convertBoundary2Map(List<String> copyBoundaryParts) {

        //创建一个总集合
        Map<String, String> boundaryMaps = new HashMap<String,String>();
        //处理contentID
        doDealContentId(copyBoundaryParts, boundaryMaps);
        doDealContentLocation(copyBoundaryParts, boundaryMaps);
        return boundaryMaps;

    }

    /**
     * 遍历{@code copyBoundaryParts}的内容，寻找到所有存在着Content-Location的part，放入{@code boundaryMaps}里
     *
     * @param copyBoundaryParts
     * @param boundaryMaps
     */
    private void doDealContentLocation(List<String> copyBoundaryParts, Map<String, String> boundaryMaps) {

        Iterator<String> iterator = copyBoundaryParts.iterator();
        while (iterator.hasNext()) {
            String part = iterator.next();
            String[] all = part.split(CONTENT_LOCATION_REGEX);
            if (all.length == 0 || all.length == 1) {
                log.warn("{}不存在Content-Location,将不放入boundaryMaps集合");
            } else {

                try {
                    String location = getLocation(part);
                    if (!StringUtils.deleteWhitespace(location).startsWith("http://")) {
                        String content = reductionContent(part);
                        boundaryMaps.put(location, content);
                        iterator.remove();
                    }
                } catch (Exception e) {
                    log.warn("{}获取Content-Location异常,将不放入boundaryMaps集合。异常为：{}", part, e);
                }
            }
        }

    }

    /**
     * 遍历{@code copyBoundaryParts}的内容，寻找到所有存在着Content-ID的part，放入{@code boundaryMaps}里
     *
     * @param copyBoundaryParts mhtml的内容集合
     * @param boundaryMaps      key为Content-ID、Content-Location，value为内容的map
     */
    private void doDealContentId(List<String> copyBoundaryParts, Map<String, String> boundaryMaps) {

        Iterator<String> iterator = copyBoundaryParts.iterator();
        while (iterator.hasNext()) {
            String part = iterator.next();
            String[] all = part.split(CONTENT_ID);
            if (all.length == 1) {
                log.warn("{}不存在Content-ID,将不放入boundaryMaps集合");
            } else {

                try {
                    String contentID = getContentID(part);
                    String content = reductionContent(part);
                    boundaryMaps.put(contentID, content);
                    iterator.remove();
                } catch (Exception e) {
                    log.warn("{}获取Content-ID异常,将不放入boundaryMaps集合。异常为：{}", part, e);
                }


            }

        }


    }

    /**
     * 从{@ccode part}里提取Content-ID的值
     *
     * @param part
     * @return
     */
    private String getContentID(String part) throws Exception {


        Matcher matcher = contentId.matcher(part);

        String contentId = null;

        while (matcher.find()) {

            if (StringUtils.isBlank(contentId)) {
                contentId = matcher.group();
            } else {
                throw new RuntimeException(String.format("%s 非规范的mhtml文件,发现多个符合条件的Content-ID"));
            }

        }

        if (StringUtils.isBlank(contentId)) {
            throw new Exception(String.format("%s 非规范的mhtml文件,未发现符合条件的Content-ID"));
        }

        //处理content——ID,截取内容
        contentId = StringUtils.deleteWhitespace(contentId.split("\\:")[1]);
        //处理content-id 去掉内容里的\\<和\\>
        if (contentId.startsWith("<") && contentId.endsWith(">")) {

            contentId = contentId.substring(contentId.indexOf("<") + 1, contentId.lastIndexOf(">"));
        }

        return contentId;


    }


}
