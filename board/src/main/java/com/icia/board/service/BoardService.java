package com.icia.board.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icia.board.dao.BoardDao;
import com.icia.board.dao.MemberDao;
import com.icia.board.dto.BoardDto;
import com.icia.board.dto.SearchDto;
import com.icia.board.util.PagingUtil;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BoardService {
	@Autowired
	private BoardDao bDao;
	@Autowired
	private MemberDao mDao; // 회원 point 정보 변경에 사용

	// transaction 관련
	@Autowired
	private PlatformTransactionManager manager;
	@Autowired
	private TransactionDefinition definition;

	private int lcnt = 10; // 한 화면(페이지)에 보여질 게시글 개수

	public String getBoardList(SearchDto sDto, HttpSession session, Model model) {
		log.info("getBoardList()");
		String view = "boardList";
		// DB에서 게시글 목록 가져오기
		int num = sDto.getPageNum();

		if (sDto.getListCnt() == 0) {
			sDto.setListCnt(lcnt);
		}

		sDto.setPageNum((num - 1) * sDto.getListCnt());// 페이징 10씩 처리하는 방식. ex) 1page => 10게시글
		List<BoardDto> bList = bDao.selectBoardList(sDto);
		model.addAttribute("bList", bList);

		// 페이징 처리
		sDto.setPageNum(num);
		String pageHtml = getPaging(sDto);
		model.addAttribute("paging", pageHtml);

		// 페이지 관련 내용 세션에 저장
		if (sDto.getColname() != null) {
			session.setAttribute("sdto", sDto);
		} else {
			session.removeAttribute("sdto"); // 검색을 안한 목록을 위해 삭제.
		}
		// 별개로 페이지 번호도 저장
		session.setAttribute("pageNum", num);

		return view;
	}

	private String getPaging(SearchDto sDto) {
		log.info("getPaging()");
		String pageHtml = null;

		// 전체 게시글 개수
		int maxNum = bDao.selectBoardCnt(sDto);

		int pageCnt = 3;// 페이지당 보여질 페이지번호 개수

		String listName = "boardList?"; // boardList.HTML 쪽 listNum 이랑 같음.
		if (sDto.getColname() != null) {
			// 검색 기능을 사용한 경우
			listName += "colname=" + sDto.getColname() + "&keyword=" + sDto.getKeyword() + "&";
			// <a href='/boardList?colname=b_title&keyword=3&pageNum=...'>
		}

		PagingUtil paging = new PagingUtil(maxNum, sDto.getPageNum(), sDto.getListCnt(), pageCnt, listName);

		pageHtml = paging.makePaging();

		return pageHtml;
	}

	// 게시글, 파일 저장 및 회원 정보(point) 변경
	public String boardWrite(List<MultipartFile> files, BoardDto board, HttpSession session, RedirectAttributes rttr) {
		log.info("boardWrite()");

		// 트랜젝션 상태 처리 객체
		TransactionStatus status = manager.getTransaction(definition);

		String view = null;
		String msg = null;

		try {
			//게시글 저장
			bDao.insertBoard(board);
			log.info("b_number : {}", board.getB_num());
			
			//파일 저장
			 
			//작성자의 point 수정
			
			// commit 수행
			manager.commit(status);
			view="redirect:boardList?pageNum=1";
			msg="작성 성공";
		} catch (Exception e) {
			e.printStackTrace();
			// rollback 수행
			manager.rollback(status);
			view = "redirect:writeForm";
			msg = "작성 실패";
		}
		
		rttr.addFlashAttribute("msg", msg);
		return view;
	}

}// class end
