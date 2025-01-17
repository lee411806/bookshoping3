package com.attiWell.admin.goods.controller;

import java.io.File;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.attiWell.admin.goods.service.AdminGoodsService;
import com.attiWell.common.base.BaseController;
import com.attiWell.goods.vo.GoodsVO;
import com.attiWell.goods.vo.ImageFileVO;
import com.attiWell.member.vo.MemberVO;

//컨트롤러 빈에 이름지정
@Controller("adminGoodsController")
@RequestMapping(value="/admin/goods")
public class AdminGoodsControllerImpl extends BaseController  implements AdminGoodsController{
	private static final String CURR_IMAGE_REPO_PATH = "C:\\shopping\\file_repo";
	
	@Autowired
	private AdminGoodsService adminGoodsService;
	
	
	 //@RequestParam 어노테이션은 HTTP 요청 파라미터를 컨트롤러 메소드의 매개변수에 매핑하는 데 사용
	@RequestMapping(value="/adminGoodsMain.do" ,method={RequestMethod.POST,RequestMethod.GET})
	public ModelAndView adminGoodsMain(@RequestParam Map<String, String> dateMap,
			                           HttpServletRequest request, HttpServletResponse response)  throws Exception {
		String viewName=(String)request.getAttribute("viewName");
		ModelAndView mav = new ModelAndView(viewName);
		HttpSession session=request.getSession();
		session=request.getSession();
		session.setAttribute("side_menu", "admin_mode"); //마이페이지 사이드 메뉴로 설정한다.
		
		
		 // admingoods.jsp 에서 날짜 데이터 javascript로 url로 controller에 전달
		String fixedSearchPeriod = dateMap.get("fixedSearchPeriod");
		String section = dateMap.get("section");
		String pageNum = dateMap.get("pageNum");
		String beginDate=null,endDate=null;
		
		//basecontroller에 있는 calcSearchPeriod에 넣어줘 날짜를 String으로 나눈다. basecontroller는 추후에 블로그에서 다룬다.
		String [] tempDate=calcSearchPeriod(fixedSearchPeriod).split(",");
		beginDate=tempDate[0];
		endDate=tempDate[1];
		
		//이 메서드에서 dateMap을 직접적으로 이제 사용하지 않지만 일관성을 위해서 dateMap에 날짜 넣어줌
		dateMap.put("beginDate", beginDate);
		dateMap.put("endDate", endDate);
		
		Map<String,Object> condMap=new HashMap<String,Object>();
		if(section== null) {
			section = "1";
		}
		condMap.put("section",section);
		if(pageNum== null) {
			pageNum = "1";
		}
		condMap.put("pageNum",pageNum);
		condMap.put("beginDate",beginDate);
		condMap.put("endDate", endDate);
		
		
		// GoodsVO 리스트를 만들어서 comdMap을 넣어 query문 갔다오게 하고 mav객체에 
        //넣어준후 반환, 추가로 위에서 만든 변수들 모두 mav객체에 넣어준 후 반환 -> 
        // jsp에서 확인가능
			List<GoodsVO> newGoodsList=adminGoodsService.listNewGoods(condMap);
			mav.addObject("newGoodsList", newGoodsList);
			
			String beginDate1[]=beginDate.split("-");
			String endDate2[]=endDate.split("-");
			
			// model 객체에 데이터 담아서 view로 쏴준다.
			mav.addObject("beginYear",beginDate1[0]);
			mav.addObject("beginMonth",beginDate1[1]);
			mav.addObject("beginDay",beginDate1[2]);
			mav.addObject("endYear",endDate2[0]);
			mav.addObject("endMonth",endDate2[1]);
			mav.addObject("endDay",endDate2[2]);
			
			mav.addObject("section", section);
			mav.addObject("pageNum", pageNum);
			return mav;
			
	}
	

	//MultipartHttpServletRequest: 파일 업로드를 처리하기 위해 사용되는 특수한 HttpServletRequest 이다.
	@RequestMapping(value="/addNewGoods.do" ,method={RequestMethod.POST})
	public ResponseEntity addNewGoods(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)  throws Exception {
		  
		
		//요청과 응답의 인코딩을 UTF-8로 설정합니다. --> 다양한 문자를 올바르게 처리하여 
		// 문자깨짐 방지
		multipartRequest.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=UTF-8");
		String imageFileName=null;
		
		
		//정리) 폼에서 보낸 값들을 newGoodsMap에 저장
		// HashMap 제너럴 타입 설정안해주면 기본 반환형식 object
		Map newGoodsMap = new HashMap();
		//multipartRequest.getParameterNames 반환 형태 -> enumeration(열거)
		Enumeration enu=multipartRequest.getParameterNames();
		//enu에 요소가 더있는지 확인 있으면 map에 key,value값 넣어주기
		//우선 그럼 여기서 addnewgood.jsp form에서 보낸 select, option , 이미지  key value값 다들어가 있다.
		while(enu.hasMoreElements()){
			String name=(String)enu.nextElement();
			String value=multipartRequest.getParameter(name);
			newGoodsMap.put(name,value);
		}
		
		//정리)세션에 담긴 멤버 정보에서 멤버 ID를 추출
		  //세션 생성해서 세션 객체에 담긴 memberinfo로 저장된 객체를 가져온다.
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
		String reg_id = memberVO.getMember_id();
		
		
		//정리) 이미지 파일 VO에 등록자 ID를 설정하고 이미지파일vo리스트에 추가 , 그 후 이미지파일 리스트 항목을  위에 선언한 newGoodsMap에 추가
		// basecontroller의 upload 메서드 가져옴. basecontroller 의 upload메서드는 파일을 temp폴더에 저장한다!!!!
		List<ImageFileVO> imageFileList =upload(multipartRequest);
		if(imageFileList!= null && imageFileList.size()!=0) {
			for(ImageFileVO imageFileVO : imageFileList) {
				imageFileVO.setReg_id(reg_id);
			}
			newGoodsMap.put("imageFileList", imageFileList);
		}
		
		String message = null;
		ResponseEntity resEntity = null;
		
		//http요청 및 응답헤더 다룸, 클라이언트에게 응답할 때 내용이 html형식이며 utf-8 인코딩을 사용함
		/*정리) http폼 응답헤더 설정, service로 위에 newgoodsmap보냄 
		 * 
		 * */
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		try {
			//위에서 form에서 담아뒀던 key value값들 전부 서비스 로 가게 해줌
			int goods_id = adminGoodsService.addNewGoods(newGoodsMap);
			if(imageFileList!=null && imageFileList.size()!=0) {
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					
					//임시폴더를 거쳐서 가는 이유가 이미지 저장할때 본폴더에 저장할때 오류가 나면 본폴더 초기화해야할 수 도 있으니까
					//그러면 원래 잘 저장되어있던 이미지도 다 날아가니까 우선 temp에 저장해놓고 문제 엎으면 목적지로 가게끔 한다.
					
					//File 클래스는 경로설정 하는데 사용 -> 아까 basecontroller의 upload메서드에서 temp폴더에 저장한 파일을 목적지 폴더로 옮긴다.
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(CURR_IMAGE_REPO_PATH+"\\"+goods_id);
					//파일 이미지 경로를 temp에서 goods_id인 폴더로 옮김 (아래 메서드에 의해 temp에 남아있는 파일은 삭제 됨)
					FileUtils.moveFileToDirectory(srcFile, destDir,true);
				}
			}
			
			//성공적으로 완료했을시 성공 메시지 작성하고 원하는 jsp로 가게해줌
			message= "<script>";
			message += " alert('새상품을 추가했습니다.');";
			message +=" location.href='"+multipartRequest.getContextPath()+"/admin/goods/addNewGoodsForm.do';";
			message +=("</script>");
		}catch(Exception e) {
			// 예외가 발생했을 때 동일한 조건의 temp 폴더에 저장된 파일들을 삭제
			//이를 통해 파일 업로드 과정에서 문제가 발생하면 temp 폴더에 남아 있는 임시 파일들을 정리
			if(imageFileList!=null && imageFileList.size()!=0) {
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					srcFile.delete();
				}
			}
			
			message= "<script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요');";
			message +=" location.href='"+multipartRequest.getContextPath()+"/admin/goods/addNewGoodsForm.do';";
			message +=("</script>");
			e.printStackTrace();
		}
		
		//ResponseEntity는 응답 본문, 응답 헤더, 상태 코드를 포함하는 객체로
		//이를 통해 서버에서 클라이언트로 다양한 형태의 응답을 보낼 수 있다. 
		resEntity =new ResponseEntity(message, responseHeaders, HttpStatus.OK);
		return resEntity;
	}
	
	
	//수정 폼 보여줌 modifygoodsform.jsp의 controller 역할
	@RequestMapping(value="/modifyGoodsForm.do" ,method={RequestMethod.GET,RequestMethod.POST})
	public ModelAndView modifyGoodsForm(@RequestParam("goods_id") int goods_id,
			                            HttpServletRequest request, HttpServletResponse response)  throws Exception {
		String viewName=(String)request.getAttribute("viewName");
		ModelAndView mav = new ModelAndView(viewName);
		
		Map goodsMap=adminGoodsService.goodsDetail(goods_id);
		mav.addObject("goodsMap",goodsMap);
		
		return mav;
	}
	
	
	// 실제 db 상품정보 수정
	@RequestMapping(value="/modifyGoodsInfo.do" ,method={RequestMethod.POST})
	public ResponseEntity modifyGoodsInfo( @RequestParam("goods_id") String goods_id,
			                     @RequestParam("attribute") String attribute,
			                     @RequestParam("value") String value,
			HttpServletRequest request, HttpServletResponse response)  throws Exception {
		//System.out.println("modifyGoodsInfo");
		
		Map<String,String> goodsMap=new HashMap<String,String>();
		goodsMap.put("goods_id", goods_id);
		goodsMap.put(attribute, value);
		adminGoodsService.modifyGoodsInfo(goodsMap);
		
		String message = null;
		ResponseEntity resEntity = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		message  = "mod_success";
		resEntity =new ResponseEntity(message, responseHeaders, HttpStatus.OK);
		return resEntity;
	}
	
	//이미지 수정버튼 누르면 이미지 수정됨
	@RequestMapping(value="/modifyGoodsImageInfo.do" ,method={RequestMethod.POST})
	public void modifyGoodsImageInfo(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)  throws Exception {
		//이미지 수정되면 콘솔에 뜸
		System.out.println("modifyGoodsImageInfo");
		multipartRequest.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		String imageFileName=null;
		
		
		// FORM에서 입력 값들 다 받아 온다.
		Map goodsMap = new HashMap();
		Enumeration enu=multipartRequest.getParameterNames();
		while(enu.hasMoreElements()){
			String name=(String)enu.nextElement();
			String value=multipartRequest.getParameter(name);
			goodsMap.put(name,value);
		}
		
		
		// 세션에서 사용자 정보 받아와서 MEMBER_ID 가져온다.
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
		String reg_id = memberVO.getMember_id();
		
		//정리) 이미지 파일 VO에 등록자 ID를 설정하고 이미지파일vo리스트에 추가 , 그 후 이미지파일 리스트 항목을  위에 선언한 newGoodsMap에 추가
		// basecontroller의 upload 메서드 가져옴. basecontroller 의 upload메서드는 파일을 temp폴더에 저장한다!!!!
		List<ImageFileVO> imageFileList=null;
		int goods_id=0;
		int image_id=0;
		try {
			//basecontroller에서 사용자가 입력한 파일이름, 실제 파일이름 저장한 imagevo반환
			//imageFileList 이 항목에 데이터가들어가 있으면 아무 데이터 입력하지 않아도 imageFileVO로 데이터 들어감
			imageFileList =upload(multipartRequest);
			if(imageFileList!= null && imageFileList.size()!=0) {
				for(ImageFileVO imageFileVO : imageFileList) {
					//goodsmap에 저장한 데이터 불러와서 ImageFileVO에 넣는다.
					goods_id = Integer.parseInt((String)goodsMap.get("goods_id"));
					image_id = Integer.parseInt((String)goodsMap.get("image_id"));
					imageFileVO.setGoods_id(goods_id);
					imageFileVO.setImage_id(image_id);
					imageFileVO.setReg_id(reg_id);
				}
				
			    adminGoodsService.modifyGoodsImage(imageFileList);
			    
			    //temp에있던 파일 실제 저장경로로 보낸다.
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(CURR_IMAGE_REPO_PATH+"\\"+goods_id);
					FileUtils.moveFileToDirectory(srcFile, destDir,true);
				}
			}
			//이미지 파일리스트가 없을 시 temp에 있는 사진 지운다.
		}catch(Exception e) {
			if(imageFileList!=null && imageFileList.size()!=0) {
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					srcFile.delete();
				}
			}
			e.printStackTrace();
		}
		
	}
	
	
	
	//이미지 추가하고 버튼 누르면 이미지 추가됨
	@Override
	@RequestMapping(value="/addNewGoodsImage.do" ,method={RequestMethod.POST})
	public void addNewGoodsImage(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {
		//추가하면 콜솔에 뜸
		System.out.println("addNewGoodsImage");
		multipartRequest.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		String imageFileName=null;
		
		Map goodsMap = new HashMap();
		// FORM에서 입력 값들 다 받아 온다.
		Enumeration enu=multipartRequest.getParameterNames();
		while(enu.hasMoreElements()){
			String name=(String)enu.nextElement();
			String value=multipartRequest.getParameter(name);
			goodsMap.put(name,value);
		}
		
		// 세션에서 사용자 정보 받아와서 MEMBER_ID 가져온다.
		HttpSession session = multipartRequest.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
		String reg_id = memberVO.getMember_id();
		
		
		//정리) 이미지 파일 VO에 등록자 ID를 설정하고 이미지파일vo리스트에 추가 , 그 후 이미지파일 리스트 항목을  위에 선언한 newGoodsMap에 추가
		// basecontroller의 upload 메서드 가져옴. basecontroller 의 upload메서드는 파일을 temp폴더에 저장한다!!!!
		List<ImageFileVO> imageFileList=null;
		int goods_id=0;
		try {
			imageFileList =upload(multipartRequest);
			if(imageFileList!= null && imageFileList.size()!=0) {
				for(ImageFileVO imageFileVO : imageFileList) {
					goods_id = Integer.parseInt((String)goodsMap.get("goods_id"));
					imageFileVO.setGoods_id(goods_id);
					imageFileVO.setReg_id(reg_id);
				}
				
			    adminGoodsService.addNewGoodsImage(imageFileList);
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					File destDir = new File(CURR_IMAGE_REPO_PATH+"\\"+goods_id);
					FileUtils.moveFileToDirectory(srcFile, destDir,true);
				}
			}
		}catch(Exception e) {
			if(imageFileList!=null && imageFileList.size()!=0) {
				for(ImageFileVO  imageFileVO:imageFileList) {
					imageFileName = imageFileVO.getFileName();
					File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+"temp"+"\\"+imageFileName);
					srcFile.delete();
				}
			}
			e.printStackTrace();
		}
	}
	
	
	//DB에서  이미지 정보삭제, 파일시스템에서 이미지파일 삭제
	@Override
	@RequestMapping(value="/removeGoodsImage.do" ,method={RequestMethod.POST})
	public void  removeGoodsImage(@RequestParam("goods_id") int goods_id,
			                      @RequestParam("image_id") int image_id,
			                      @RequestParam("imageFileName") String imageFileName,
			                      HttpServletRequest request, HttpServletResponse response)  throws Exception {
		
		adminGoodsService.removeGoodsImage(image_id);
		try{
			File srcFile = new File(CURR_IMAGE_REPO_PATH+"\\"+goods_id+"\\"+imageFileName);
			srcFile.delete();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	// db에서 상품정보 삭제, 상품삭제 후 리다이렉트
	@Override
	@RequestMapping(value="/removeGoods.do" ,method={RequestMethod.GET})
	public void removeGoods(@RequestParam("goods_id") int goods_id,
			                      HttpServletRequest request, HttpServletResponse response)  throws Exception {
		
		adminGoodsService.removeGoods(goods_id);
		
		response.sendRedirect(request.getContextPath() + "/admin/goods/adminGoodsMain.do");
	
		
	}
	
	
	

}
