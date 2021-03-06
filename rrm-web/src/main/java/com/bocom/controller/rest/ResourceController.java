package com.bocom.controller.rest;

import com.bocom.domain.ResourceInfo;
import com.bocom.dto.AdvancedDto;
import com.bocom.dto.ResourceInfoDto;
import com.bocom.dto.img.ImageListDto;
import com.bocom.service.ImageFormatService;
import com.bocom.service.OperationService;
import com.bocom.service.ResourceInfoService;
import com.bocom.service.api.PapService;
import com.bocom.util.*;
import com.github.pagehelper.PageInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

@Controller
@RequestMapping("/manager/resource")
public class ResourceController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceInfoService resourceInfoService;
    @Autowired
    private OperationService operationService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ImageFormatService imageFormatService;

    @Autowired
    private PapService papService;

    @Value("${myself.imageFlie.dir}")
    private String fileDir;

    /**
     * 查询资源,带分页
     *
     * @return
     */
    @RequestMapping("/queryByResource")
    @ResponseBody
    public Map queryByResource(@RequestBody Map map) {
        PageInfo pageInfo = null;
        try {
            logger.info("queryResource传递的参数为==============================>" + map);
            PageUtil.setParams1(request, map);
            List<ResourceInfoDto> list = resourceInfoService.queryByResource(map);
            pageInfo = new PageInfo(list);
        } catch (Exception e) {
            logger.info("queryResource==============> 出错" + e);
        }
        return PageUtil.covertMap(new Object[]{"page"}, new Object[]{pageInfo});
    }


    @RequestMapping("/queryResource")
    @ResponseBody
    public Map queryResource(@RequestBody Map map) {
        PageInfo pageInfo = null;
        try {
            logger.info("queryResource传递的参数为==============================>" + map);
            PageUtil.setParams1(request, map);
            List<ResourceInfoDto> list = resourceInfoService.queryResource(map);
            pageInfo = new PageInfo(list);
        } catch (Exception e) {
            logger.info("queryResource==============> 出错" + e);
        }
        return PageUtil.covertMap(new Object[]{"page"}, new Object[]{pageInfo});
    }

    /**
     * 查询资源上传总数量，已经昨日上传数量
     *
     * @return
     */
    @RequestMapping("/queryResourceSize")
    @ResponseBody
    public ResponseVo queryResourceSize(@RequestBody Map map,HttpServletRequest request) {
        PageInfo pageInfo = null;
        try {
            logger.info("queryResourceSize==============================>" + map);
            String sessionid = request.getSession().getId();
            map.put("sessionid", sessionid);
            return resourceInfoService.queryResourceSize(map);
        } catch (Exception e) {
            logger.info("queryResource==============> 出错" + e);
            return ResponseVoUtil.fail("查询资源数据失败", e);
        }

    }

    /**
     * 更新上传的资源
     *
     * @return
     */
    @RequestMapping("/updateResource")
    @ResponseBody
    public ResponseVo updateResource(@RequestBody ResourceInfo resourceInfo) {
        try {
            logger.info("updateResource获取到的id为==============================>" + resourceInfo.getId());
            return ResponseVoUtil.success(resourceInfoService.updateResourceInfo(resourceInfo));
        } catch (Exception e) {
            logger.info("updateResource==============> 出错" + e);
            return ResponseVoUtil.fail("修改信息错误", e);
        }

    }

    /**
     * 删除上传的资源
     *
     * @return
     */
    @RequestMapping("/deleteResource")
    @ResponseBody
    public ResponseVo deleteResource(String ids) {
        try {
            List<String> idList = new ArrayList<>();
            if (null == ids) {
                return ResponseVoUtil.fail("失败", "参数不对");
            }
            if (ids.indexOf(",") != -1) {
                String[] idArray = ids.split(",");
                idList = Arrays.asList(idArray);
            } else {
                idList.add(ids);
            }
            for (String id : idList) {
                ResourceInfo resourceInfo = new ResourceInfo();
                resourceInfo.setId(Integer.parseInt(id));
                logger.info("deleteResource==============================>" + resourceInfo.getId());
                resourceInfoService.deleteResource(resourceInfo);
            }
            return ResponseVoUtil.success("成功");
        } catch (Exception e) {
            logger.info("deleteResource==============> 出错" + e);
            return ResponseVoUtil.fail("删除信息错误", e);
        }

    }

    /**
     * 下载文件
     *
     * @param ids
     * @param type
     * @param response
     */
    @RequestMapping("/dowloadFile")
    @ResponseBody
    public void dowloadFile(String ids, String type, HttpServletResponse response) {
        List<ImageListDto> list = new ArrayList<>();
        List<String> idList = new ArrayList<>();
        try {
            if (null == ids) {
                return;
            }
            if (ids.indexOf(",") != -1) {
                String[] idArray = ids.split(",");
                idList = Arrays.asList(idArray);
            } else {
                idList.add(ids);
            }
            Map map = new HashMap();
            for (String id : idList) {
                logger.info("下载传入的id为===========>" + id);
                map.put("id", id);
                List<ResourceInfoDto> resourceInfoDtoList = resourceInfoService.queryResource(map);
                ResourceInfoDto resourceInfoDto = resourceInfoDtoList.get(0);
                ImageListDto imageListDto = new ImageListDto();
                imageListDto.setName(resourceInfoDto.getResourceName());
                String path = resourceInfoDto.getStoragePath();
                imageListDto.setUrl(path.substring(path.indexOf("group"), path.length()));
                list.add(imageListDto);
                operationService.addOperation(Integer.parseInt(id));
            }
            String str = imageFormatService.downLoadFileToServer(list,
                    UserUtils.getUserName(request.getSession()), UserUtils.isAdmin(request.getSession()));
            String fileName = str.substring(str.lastIndexOf(File.separator) + 1, str.length());
            FileUtil.fileToZip(str, fileDir, fileName);
            File zipFile = new File(fileDir + File.separator + fileName + "" + ".zip");
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) zipFile.length());
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(zipFile));
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = in.read(b)) != -1) {
                bos.write(b, 0, len);
            }
            // 得到文件的二进制数组
            byte[] fileByte = bos.toByteArray();
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            // 设置response的Header
            response.setContentType("application/x-msdownload");
            // 如果输出的是中文名的文件，在此处就要用URLEncoder.encode方法进行处理
            response.setHeader(
                    "Content-Disposition",
                    "attachment;filename="
                            + URLEncoder.encode(str.substring(str.lastIndexOf(File.separator) + 1, str.length()) + ""
                            + ".zip", "UTF-8"));
            toClient.write(fileByte);
            toClient.flush();
            toClient.close();

        } catch (Exception e) {
            logger.error("下载文件出现错误", e);

        }

    }

    /**
     * 上传文件
     *
     * @return
     */
    @RequestMapping("/uploadFile")
    public void uploadFile(@RequestParam MultipartFile fileList, @RequestParam(required = false) String resourceName,
                           @RequestParam(required = false) String resourceType, @RequestParam(required = false)
                                   String resourceKey,
                           @RequestParam(required = false) String resourcePlace, @RequestParam(required = false)
                                   String taskYear,
                           @RequestParam(required = false) String adminDivision, @RequestParam(required = false)
                                   String taskName, @RequestParam(required = false)
                                   String taskId, HttpServletResponse response) {
        // 首先根据文件数组操作文件
        try {
            ResourceInfo resourceInfo = new ResourceInfo();
            resourceInfo.setResourceName(resourceName);
            resourceInfo.setResourceKey(resourceKey);
            resourceInfo.setResourceType(resourceType);
            resourceInfo.setResourcePlace(resourcePlace);
            resourceInfo.setTaskYear(taskYear);
            resourceInfo.setAdminDivision(adminDivision);
            resourceInfo.setTaskName(taskName);
            resourceInfo.setTaskId(taskId);
            writePrint(resourceInfoService.uploadFile(fileList, resourceInfo), response);
//            writePrint(ResponseVoUtil.success("上传成功"), response);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }


    /**
     * 上传微应用图片
     *
     * @param file
     * @param response
     */
    @RequestMapping("/uploadPhoto")
    public void uploadLogoWebPhoto(@RequestParam("logoWeb") MultipartFile file,
                                   HttpServletResponse response) {
        try {
            ResponseVo responseVo = new ResponseVo();
            writePrint(responseVo, response);
        } catch (Exception e) {
            logger.error("上传图片错误：" + e.getMessage());
        }
    }


    /**
     * 区划
     *
     * @return
     */
    @RequestMapping("/areaList")
    @ResponseBody
    public ResponseVo areaList(String code) {
        // 首先根据文件数组操作文件
        try {
            return papService.areaList(code);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private static void writePrint(ResponseVo responseVo, HttpServletResponse resp) {
        resp.setContentType("text/plain; charset=UTF-8");
        try {
            String json = JsonUtil.toJSon(responseVo);
            resp.getWriter().print(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/downUploadRecord")
    public void downUploadRecord(HttpServletRequest request, HttpServletResponse response) {
        String filePath = request.getParameter("path");
        try {
            String fileId = "";
            if (filePath.indexOf("http") != -1) {
                fileId = filePath.substring(filePath.indexOf("group"), filePath.length());
            }
            Map map = new HashMap();
            map.put("storagePath", fileId);
            List<ResourceInfoDto> dtos = resourceInfoService.queryServiceInfo(map);
            if(dtos != null && dtos.size()>0){
                operationService.addOperation(dtos.get(0).getId());
            }
            String suffix = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String tempFilePath = fileDir + File.separator + uuid;
            File targetFile = new File(tempFilePath);
            targetFile.mkdirs();
            String targetFilePath = tempFilePath + File.separator + uuid + "." + suffix;
            // 获取文件的二进制数组
            byte[] buffer = new FastDfsUtil().downloadFile(fileId);
            boolean waterFlag = false;
            if (!UserUtils.isAdmin(request.getSession())) {
                waterFlag = FileUtil.addWaterMarkOnFile(buffer, filePath, targetFilePath, UserUtils.getUserName
                        (request.getSession()));
            }

            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            // 设置response的Header
            response.setContentType("application/x-msdownload");
            // 如果输出的是中文名的文件，在此处就要用URLEncoder.encode方法进行处理
            response.setHeader("Content-Disposition", "attachment;filename="
                    + URLEncoder.encode(fileId, "UTF-8"));
            if (waterFlag) {
                File dowFile = new File(targetFilePath);
                toClient.write(FileUtil.fileToByte(dowFile));
            } else {
                toClient.write(buffer);
            }
            toClient.flush();
            toClient.close();
            FileUtil.deleteDir(targetFile);
        } catch (Exception e) {
            logger.error("系统异常{}", e);
        }
    }


    /**
     * 高级搜索
     *
     * @param advancedDto
     * @return
     */
    @RequestMapping("/advancedSearch")
    @ResponseBody
    public Map advancedSearch(@RequestBody AdvancedDto advancedDto) {
        PageInfo pageInfo = null;
        try {
            PageUtil.setParams2(request, advancedDto);
            List<ResourceInfoDto> list = resourceInfoService.advancedSearch(advancedDto);
            pageInfo = new PageInfo(list);
        } catch (Exception e) {
            logger.error("系统异常{}", e);
        }
        return PageUtil.covertMap(new Object[]{"page"}, new Object[]{pageInfo});
    }

    /**
     * 查询服务信息
     *
     * @param advancedDto
     * @return
     */
    @RequestMapping("/queryServiceInfo")
    @ResponseBody
    public Map queryServiceInfo(HttpServletRequest request) {
        PageInfo pageInfo = null;
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("status","2");
            PageUtil.setParams(request, map);
            List<ResourceInfoDto> list = resourceInfoService.queryServiceInfo(map);
            pageInfo = new PageInfo(list);
        } catch (Exception e) {
            logger.error("系统异常{}", e);
        }
        return PageUtil.covertMap(new Object[]{"page"}, new Object[]{pageInfo});
    }
    
//    @RequestMapping("/queryCas")
//    @ResponseBody
//    public ResponseVo queryResourceSize(HttpServletRequest request, HttpServletResponse response) {
//        try {
//         // _const_cas_assertion_是CAS中存放登录用户名的session标志
//            Object object = request.getSession().getAttribute(
//                    "_const_cas_assertion_");
//            response.sethsetHeader("_const_cas_assertion_", object);
//        } catch (Exception e) {
//            logger.info("queryResource==============> 出错" + e);
//            return ResponseVoUtil.fail("查询资源数据失败", e);
//        }
//
//    }

}
