package eun.myself.myapp.board.controller;

import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import eun.myself.myapp.board.model.Board;
import eun.myself.myapp.board.model.BoardCategory;
import eun.myself.myapp.board.model.BoardUploadFile;
import eun.myself.myapp.board.service.IBoardCategoryService;
import eun.myself.myapp.board.service.IBoardService;
import eun.myself.myapp.syslog.SysLog;

@Controller
public class BoardController {

	@Autowired
	IBoardService boardService;
	
	@Autowired
	IBoardCategoryService categoryService;
	
	@Autowired
	SysLog syslog;

	
	@RequestMapping("/boardList/cat/{category_id}/{page}")
	public String getListByCategory(@PathVariable int category_id, @PathVariable int page, HttpSession session, Model model)
	{
		/*
		 * �Խù��� ī�װ�id���� �������  �Խù��� �ҷ����� ������������ �Խù��� ������ �����ش�.
		 * 
		 */
		//�Խù� ī�װ� ����Ʈ�� �ҷ����� ������
	
		//ī�װ� ���̵� ���� �������� ������ �޴´�.
		//���ǿ� �������� ��Requst�� ī�װ� ���̵� �ִ´�.
		session.setAttribute("page", page);
		model.addAttribute("category_id", category_id);
	
		List<Board> boardList = boardService.selectArticleListByCategory(category_id, page);
		model.addAttribute("boardList", boardList);

		// paging start
		int bbsCount = boardService.selectTotalArticleCountByCategoryId(category_id);
		int totalPage = 0;

		if(bbsCount > 0) {
			totalPage= (int)Math.ceil(bbsCount/10.0);
		}
	
		model.addAttribute("totalPageCount", totalPage);
		model.addAttribute("page", page);
	
		return "boardList/boardList";
	}
	
	@RequestMapping(value = "/boardList/cat/{category_id}")
	public String getListByCategory(@PathVariable int category_id,HttpSession session, Model model) {
		/*
		 * ī�װ��� ���õ� �Խù��� �����ִµ� �������� ���������ʾ� ù�������� ȣ���� �����ش�.
		 */
		//syslog.getLog("->boardList/cat/"+category_id);
		return getListByCategory(category_id, 1, session, model);
	}
	
	@RequestMapping("/board/{board_id}/{page}")	
	public String getBoardDetails(@PathVariable int board_id, @PathVariable int page, Model model) {
		//syslog.getLog("1");
		//�Խù��� ������ �������� ���� ����� �� �ֵ��� request���� �Է�
		Board board = boardService.selectArticle(board_id);
		model.addAttribute("board", board);
		model.addAttribute("page", page);
		model.addAttribute("category_id", board.getCategory_id());
	//	syslog.getLog("�� �ƺ���"+board.getReply_parents_number());
		//logger.info("getBoardDetails " + board.toString());
		return "boardView/boardView";
	}

	@RequestMapping("/board/{board_id}")
	public String getBoardDetails(@PathVariable int board_id, Model model) {
		//�Խù� ������ �ҷ����� ����Ʈ ������ 1�� �Է�
	//	syslog.getLog("1");
		return getBoardDetails(board_id, 1, model);
	}
	
	@RequestMapping(value="/boardWrite/{category_id}", method=RequestMethod.GET)
	public String writeArticle(@PathVariable int category_id, Model model) {
		/*ī�װ����� �ҷ��´����� �Խù� ������ ���û��� ����Ʈ�� ���� �ִ´� �Խù� ���� ������ ȣ��
		 * ī�װ� ���̵�� �ʱ� ���� �������� �����ϱ� ���� �̰� �Խù� ���� ������ ȣ��
		 */
		
		List<BoardCategory> categoryList = categoryService.selectAllCategory();
		model.addAttribute("categoryList", categoryList);
		model.addAttribute("category_id", category_id);
		return "boardWrite/boardWrite";
	}
	

	@RequestMapping(value="/boardWrite", method=RequestMethod.POST)
	public String writeArticle(Board board, BindingResult result, RedirectAttributes redirectAttrs) {
		/*
		 * �Խù����� ���� �Խñ��� �ۼ��Ѵ�.
		 * �ۼ��� �ڿ��� �Խù� ������� ���ƿ´�.
		 */
		syslog.getLog("/boardWrite : " + board.toString());
		syslog.getLog("board_id : "+board.getBoard_id());
		syslog.getLog("writer_id : "+board.getWriter_id());
		syslog.getLog("board_passwd : "+board.getBoard_password());
		try{
			syslog.getLog("title:"+board.getTitle());
			board.setTitle(Jsoup.clean(board.getTitle(), Whitelist.basic()));
		
			board.setContent(Jsoup.clean(board.getContent(), Whitelist.basic()));
	
			MultipartFile mfile = board.getFile();

			if(mfile!=null && !mfile.isEmpty()) {
				syslog.getLog("/boardWrite : " + mfile.getOriginalFilename());
				BoardUploadFile file = new BoardUploadFile();
				file.setFile_name(mfile.getOriginalFilename());
				file.setFile_size(mfile.getSize());
				file.setFile_content_type(mfile.getContentType());
				file.setFile_data(mfile.getBytes());
				syslog.getLog("/boardWrite : " + file.toString());
	
				boardService.insertArticle(board, file);
			}else {
		
				boardService.insertArticle(board);
			}
		}catch(Exception e){
			e.printStackTrace();
			redirectAttrs.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/boardList/cat/"+board.getCategory_id();
	}
	
	@RequestMapping(value="/boardDelete/{board_id}", method=RequestMethod.GET)
	public String deleteArticle(@PathVariable int board_id, Model model) {
		/*
		 * �Խù� ���̵� ���� �������� ���� �Խù��� ������ �������� �������� �������� ȣ�� 
		 */
		Board board = boardService.selectDeleteArticle(board_id);
		model.addAttribute("category_id", board.getCategory_id());
		model.addAttribute("board_id", board_id);
		model.addAttribute("reply_number", board.getReply_number());
		model.addAttribute("master_id",board.getMaster_id());
		return "boardDelete/boardDelete";
	}
	
	@RequestMapping(value="/boardDelete", method=RequestMethod.POST)
	public String deleteArticle(Board board, BindingResult result, HttpSession session, Model model) {
		/*
		 * ���������������� db�� �Խù� ���̵� ����� ��й�ȣ�� ���� �� 
		 * �Խù� ���̵� ���� �������� ������ �����Ѵ�. ��ۿ� ���ؼ��� �߰������� �Է¹޾� ���� �����Ѵ�.
		 */
		syslog.getLog("�����ͻ���:"+board.getMaster_id());
		try {
			
			String dbpw = boardService.getPassword(board.getBoard_id());

			if(dbpw.equals(board.getBoard_password())) {
				boardService.deleteArticle(board.getBoard_id(),board.getMaster_id(), board.getReply_number());
				return "redirect:/boardList/cat/"+board.getCategory_id()+"/"+(Integer)session.getAttribute("page");
			}else {
				model.addAttribute("message", "WRONG_PASSWORD_NOT_DELETED");
				return "error/runtime";
			}
		}catch(Exception e){
			model.addAttribute("message", e.getMessage());
			e.printStackTrace();
			return "error/runtime";
		}
		
	}
	
	
	@RequestMapping(value="/boardUpdate/{board_id}", method=RequestMethod.GET)
	public String updateArticle(@PathVariable int board_id, Model model) {
		/*
		 * �Խù� ���� �Խù����̵� ��� ���� ������ ȣ�� �Ѵ� ī�װ� ������ ���� �ҷ��� �� ���õǾ��ִ� ī�װ��� 
		 * �Խù� ������ �ѱ��.
		 */
		List<BoardCategory> categoryList = categoryService.selectAllCategory();
		model.addAttribute("categoryList", categoryList);
		Board board = boardService.selectArticle(board_id);
		model.addAttribute("category_id", board.getCategory_id());
		model.addAttribute("board", board);
		return "boardUpdate/boardUpdate";
	}

	@RequestMapping(value="/boardUpdate", method=RequestMethod.POST)
	public String updateArticle(Board board, BindingResult result, HttpSession session, RedirectAttributes redirectAttrs) {
		//logger.info("/boardUpdate " + board.toString());
		try{
			board.setTitle(Jsoup.clean(board.getTitle(), Whitelist.basic()));
			board.setContent(Jsoup.clean(board.getContent(), Whitelist.basic()));
			MultipartFile mfile = board.getFile();
			if(mfile!=null && !mfile.isEmpty()) {
				//logger.info("/board/update : " + mfile.getOriginalFilename());
				BoardUploadFile file = new BoardUploadFile();
				file.setFile_id(board.getFile_id());
				file.setFile_name(mfile.getOriginalFilename());
				file.setFile_size(mfile.getSize());
				file.setFile_content_type(mfile.getContentType());
				file.setFile_data(mfile.getBytes());
				//logger.info("/board/update : " + file.toString());
				boardService.updateArticle(board, file);
			}else {
				boardService.updateArticle(board);
			}
		}catch(Exception e){
			e.printStackTrace();
			redirectAttrs.addFlashAttribute("message", e.getMessage());
		}

		return "redirect:/board/"+board.getBoard_id();
	}
	@RequestMapping(value="/boardReply/{board_id}",method=RequestMethod.GET)
	public String replyArticle(@PathVariable int board_id, Model model) {
		/*
		 * �Խù� id ���� �������� ����� �ۼ��Ѵ�.
		 * 
		 */
		Board board = boardService.selectArticle(board_id);
		board.setWriter("");
		board.setWriter_id("");
		board.setTitle("[Re]"+board.getTitle());
		board.setContent("\n\n\n----------\n" + board.getContent());
		model.addAttribute("board", board);
		model.addAttribute("next", "reply");
		return "boardReply/boardReply";
	}
	@RequestMapping(value="/boardReply",method=RequestMethod.POST)
	public String replyArticle(Board board, BindingResult result, RedirectAttributes redirectAttrs, HttpSession session) {
		//logger.info("/board/reply : " + board.toString());

//	    if(result.hasErrors()) {
//	    	logger.debug(result.getErrorCount());
//	        return "board/write";
//	    }

		try{
			syslog.getLog("-1");
			board.setTitle(Jsoup.clean(board.getTitle(), Whitelist.basic()));
			board.setContent(Jsoup.clean(board.getContent(), Whitelist.basic()));
			syslog.getLog("-2");
			syslog.getLog(""+board.getReply_number());
			MultipartFile mfile = board.getFile();
			if(mfile!=null && !mfile.isEmpty()) {
				//logger.info("/board/reply : " + mfile.getOriginalFilename());
				BoardUploadFile file = new BoardUploadFile();
				file.setFile_name(mfile.getOriginalFilename());
				file.setFile_size(mfile.getSize());
				file.setFile_content_type(mfile.getContentType());
				file.setFile_data(mfile.getBytes());
				//logger.info("/board/reply : " + file.toString());

				boardService.replyArticle(board, file);
			}else {
				
				boardService.replyArticle(board);
				syslog.getLog("-3");
			}
		}catch(Exception e){
			e.printStackTrace();
			redirectAttrs.addFlashAttribute("message", e.getMessage());
		}
		
		if(session.getAttribute("page") != null) {
			return "redirect:/boardList/cat/"+board.getCategory_id()+"/"+(Integer)session.getAttribute("page");
		}else {
			return "redirect:/boardList/cat/"+board.getCategory_id(); 
		}
	}
	@RequestMapping("/file/{file_id}")
	public ResponseEntity<byte[]> getFile(@PathVariable int file_id) {
		BoardUploadFile file = boardService.getFile(file_id);
		//logger.info("getFile " + file.toString());
		final HttpHeaders headers = new HttpHeaders();
		
		String[] mtypes = file.getFile_content_type().split("/");
		headers.setContentType(new MediaType(mtypes[0], mtypes[1]));
		headers.setContentLength(file.getFile_size());
		headers.setContentDispositionFormData("attachment", file.getFile_name(), Charset.forName("UTF-8"));
		return new ResponseEntity<byte[]>(file.getFile_data(), headers, HttpStatus.OK);
	}
	@RequestMapping("/boardSearch/{page}")
	public String search(@RequestParam(required=false, defaultValue="") String keyword, @PathVariable int page, HttpSession session, Model model) {
		try {
			List<Board> boardList = boardService.searchListByContentKeyword(keyword, page);
			model.addAttribute("boardList", boardList);
	
			// paging start
			int bbsCount = boardService.selectTotalArticleCountByKeyword(keyword);
			int totalPage = 0;
			//System.out.println(bbsCount);
			if(bbsCount > 0) {
				totalPage= (int)Math.ceil(bbsCount/10.0);
			}
			model.addAttribute("totalPageCount", totalPage);
			model.addAttribute("page", page);
			model.addAttribute("keyword", keyword);
			//logger.info(totalPage + ":" + page + ":" + keyword);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "boardSearch/boardSearch";
	}
}
