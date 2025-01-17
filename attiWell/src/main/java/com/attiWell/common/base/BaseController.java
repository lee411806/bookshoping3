package com.attiWell.common.base;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import com.attiWell.goods.vo.ImageFileVO;

public abstract class BaseController  {
   private static final String CURR_IMAGE_REPO_PATH = "C:\\shopping\\file_repo";
   
   protected List<ImageFileVO> upload(MultipartHttpServletRequest multipartRequest) throws Exception{
      List<ImageFileVO> fileList= new ArrayList<ImageFileVO>();
      // multipartRequest.getFileNames(); filename이 폼에서 사용자가 입력한 파일이름이다.
      Iterator<String> fileNames = multipartRequest.getFileNames();
      while(fileNames.hasNext()){
    	  
    	  //사용자가 입력한 파일이름
         ImageFileVO imageFileVO =new ImageFileVO();
         String fileName = fileNames.next();
         imageFileVO.setFileType(fileName);
         
         // multipartRequest.getFile(fileName); 실제 파일이름 저장
         MultipartFile mFile = multipartRequest.getFile(fileName);
         String originalFileName=mFile.getOriginalFilename();
         imageFileVO.setFileName(originalFileName);
         fileList.add(imageFileVO);
         
         //File 클래스 사용해서 temp폴더에 실제 폴더 전송
         File file = new File(CURR_IMAGE_REPO_PATH +"\\"+ fileName);
         if(mFile.getSize()!=0){ //File Null Check
            if(! file.exists()){ //경로상에 파일이 존재하지 않을 경우
               if(file.getParentFile().mkdirs()){ //경로에 해당하는 디렉토리들을 생성
                     file.createNewFile(); //이후 파일 생성
               }
            }
            mFile.transferTo(new File(CURR_IMAGE_REPO_PATH +"\\"+"temp"+ "\\"+originalFileName)); //임시로 저장된 multipartFile을 실제 파일로 전송
         }
      }
      return fileList;
   }
   
   private void deleteFile(String fileName) {
      File file =new File(CURR_IMAGE_REPO_PATH+"\\"+fileName);
      try{
         file.delete();
      }catch(Exception e){
         e.printStackTrace();
      }
   }
   
   
   @RequestMapping(value="/*.do" ,method={RequestMethod.POST,RequestMethod.GET})
   protected  ModelAndView viewForm(HttpServletRequest request, HttpServletResponse response) throws Exception {
      String viewName=(String)request.getAttribute("viewName");
      ModelAndView mav = new ModelAndView(viewName);
      return mav;
   }
   
   
   protected String calcSearchPeriod(String fixedSearchPeriod){
      String beginDate=null;
      String endDate=null;
      String endYear=null;
      String endMonth=null;
      String endDay=null;
      String beginYear=null;
      String beginMonth=null;
      String beginDay=null;
      // 두자리로 포맷
      DecimalFormat df = new DecimalFormat("00");
      Calendar cal=Calendar.getInstance();
      
      //cal객체를 통해 현재 년월일 반환 
      endYear   = Integer.toString(cal.get(Calendar.YEAR));
      endMonth  = df.format(cal.get(Calendar.MONTH) + 1);
      endDay   = df.format(cal.get(Calendar.DATE));
      endDate = endYear +"-"+ endMonth +"-"+endDay;
      
      if(fixedSearchPeriod == null) {
         cal.add(cal.MONTH,-4);
      }else if(fixedSearchPeriod.equals("one_week")) {
         cal.add(Calendar.DAY_OF_YEAR, -7);
      }else if(fixedSearchPeriod.equals("two_week")) {
         cal.add(Calendar.DAY_OF_YEAR, -14);
      }else if(fixedSearchPeriod.equals("one_month")) {
         cal.add(cal.MONTH,-1);
      }else if(fixedSearchPeriod.equals("two_month")) {
         cal.add(cal.MONTH,-2);
      }else if(fixedSearchPeriod.equals("three_month")) {
         cal.add(cal.MONTH,-3);
      }else if(fixedSearchPeriod.equals("six_month")) {
         cal.add(cal.MONTH,-6);
      }
      
      //현재 날짜에 사용자가 선택한 날짜를 빼서 시작날짜 구함
      beginYear   = Integer.toString(cal.get(Calendar.YEAR));
      beginMonth  = df.format(cal.get(Calendar.MONTH) + 1);
      beginDay   = df.format(cal.get(Calendar.DATE));
      beginDate = beginYear +"-"+ beginMonth +"-"+beginDay;
      
      return beginDate+","+endDate;
   }
   
}