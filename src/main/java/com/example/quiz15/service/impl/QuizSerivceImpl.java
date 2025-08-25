package com.example.quiz15.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.constants.QuestionType;
import com.example.quiz15.constants.ResCodeMessage;
import com.example.quiz15.dao.QuestionDao;
import com.example.quiz15.dao.QuizDao;
import com.example.quiz15.entity.Question;
import com.example.quiz15.entity.Quiz;
import com.example.quiz15.service.ifs.QuizService;
import com.example.quiz15.vo.BasicRes;
import com.example.quiz15.vo.QuestionRes;
import com.example.quiz15.vo.QuestionVo;
import com.example.quiz15.vo.QuizCreateReq;
import com.example.quiz15.vo.QuizUpdateReq;
import com.example.quiz15.vo.SearchReq;
import com.example.quiz15.vo.SearchRes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizSerivceImpl implements QuizService {
	// 提供 類別 (或 Json 格式) 與物件之間的轉換
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private QuizDao quizDao;

	@Autowired
	private QuestionDao questionDao;

	/**
	 * @throws Exception
	 * @Transactional :事務</br>
	 *                1.當一個方法中執行多個Dao時 (跨表或是同一張表寫多筆資料) 這些所有的資料應該都要算同一次的行為
	 *                所以這些資料要嘛全部成功 不然就全部寫入失敗</br>
	 *                2. @Transactional 有效的回朔的異常預設是 RunTimeException 若發生的異常不是
	 *                RunTimeException 或其子類別的異常類型 資料皆不會回朔 因此想要讓只要發生任何一種異常時資料都要可以回朔
	 *                可以 將 @Transactional 的有效範圍從 RunTimeException 提高至 Exception
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes create(QuizCreateReq req) throws Exception {
		// 參數檢查已透過 @Valid 驗證
		try {
			// 檢查日期 使用排除法
			BasicRes checkRes=checkDate(req.getStartDate(), req.getEndDate());
			if(checkRes!=null) {
				return checkRes;
			}
			
			// 1 開始日期不能比結束日期晚 2 開始日期不能比當前創建的日期早
			// 判斷式 : 假設開始日期比結束日期晚 或 開始日期比當前日期早 --> 回饋錯誤訊息
			// LocalDate.now() --> 取得當前的日期
//			if (req.getStartDate().isAfter(req.getEndDate()) //
//					|| req.getStartDate().isBefore(LocalDate.now())) {
//				return new BasicRes(ResCodeMessage.DATE_FORMAT_ERROR.getCode(), //
//						ResCodeMessage.DATE_FORMAT_ERROR.getMessage());
//			}
			
			// 新增問卷
			quizDao.insert(req.getName(), req.getDescription(), req.getStartDate(), //
					req.getEndDate(), req.isPublished());
			// 新增完問卷後 取得流水號
			// 雖然因為 @Transactional 尚未將資料提交(commit)進資料庫，但實際上SQL語法已經執行完畢，
			// 依然可以取得對應的值
			int quizId = quizDao.getMaxQuizId();
			// 新增問題
			List<QuestionVo> questionVoList = req.getQuestionList();
			// 處理每一問題
			for (QuestionVo vo : questionVoList) {
				// 檢查題目類型 與 選項
				checkRes = checkQuestionType(vo);
				// 呼叫方法 checkQuestionType 得到的res 若是null 表示檢查都沒問題
				// 因為方法中檢查到最後都沒問題時是回傳 null
				if (checkRes != null) {
					// return checkRes;
					// 因為前面已經執行了 quizDao.insert 了，所以這邊要拋出 Exception
					// 才會讓 @Transactional 生效
					throw new Exception(checkRes.getMessage());
				}
				// 因為mysql 沒有list 資料格式 所以要把options 資料格式 從 List<String> 轉成 String
				List<String> optionsList = vo.getOptions();
				String str = mapper.writeValueAsString(optionsList);

				// 要記得設定 quizId
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), ResCodeMessage.SUCCESS.getMessage());

		} catch (Exception e) {
			// 不能return BasicRes 而是要將發生的異常拋出去 這樣@Transaction 才會生效
			throw e;
		}
	}

	private BasicRes checkQuestionType(QuestionVo vo) {
		// 1 檢查type 是否式規定的類型
		String type = vo.getType();
		// 假設 從 vo 取出的type 不符合定義的3種類型的其中一種 就返回錯誤訊息
		if (!type.equalsIgnoreCase(QuestionType.SINGLE.getType())//
				&& !type.equalsIgnoreCase(QuestionType.MULIT.getType())//
				&& !type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
			System.out.println(111);
			return new BasicRes(ResCodeMessage.QUESTION_TYPE_ERROR.getCode(),
					ResCodeMessage.QUESTION_TYPE_ERROR.getMessage());
		}
		// 2 type 是 單選或多選的時候 選項(options)至少要有2個
		// 假設 type 不等於 TEXT --> 就表示 type 是單選或多選
		if (!type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
			// 單選或多選時選項至少要2個
			if (vo.getOptions().size() < 2) {
				return new BasicRes(ResCodeMessage.OPTIONS_INSUFFICIENT.getCode(),
						ResCodeMessage.OPTIONS_INSUFFICIENT.getMessage());
			}
		} else { // else --> type 是 text --> 選項應該是 null 或是 size = 0
			// 假設 選項 不是null 或 選項的list中有值
			if (vo.getOptions() != null && vo.getOptions().size() > 0) {
				return new BasicRes(ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getCode(),
						ResCodeMessage.TEXT_HAS_OPTIONS_ERROR.getMessage());
			}
		}
		return null;
	}

	private BasicRes checkDate(LocalDate startDate, LocalDate endDate) {
		// 1 開始日期不能比結束日期晚 2 開始日期不能比當前創建的日期早
		// 判斷式 : 假設開始日期比結束日期晚 或 開始日期比當前日期早 --> 回饋錯誤訊息
		// LocalDate.now() --> 取得當前的日期
		if (startDate.isAfter(endDate) //
				|| startDate.isBefore(LocalDate.now())) {
			return new BasicRes(ResCodeMessage.DATE_FORMAT_ERROR.getCode(), //
					ResCodeMessage.DATE_FORMAT_ERROR.getMessage());
		}
		return null;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes update(QuizUpdateReq req) throws Exception {
		// 參數檢查已透過 @Valid 驗證
		// 更新是對已存在的問卷進行修改
		try {
			// 檢查quizId是否存在
			int quizId=req.getQuizId();
			int count = quizDao.getCountByQuizId(quizId);
			if (count != 1) {
				return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), ResCodeMessage.NOT_FOUND.getMessage());
			}
			//檢查日期
			BasicRes checkRes=checkDate(req.getStartDate(), req.getEndDate());
			if(checkRes!=null) {
				return checkRes;
			}
			//更新問卷
			int updateRes=quizDao.update(quizId, req.getName(),//
					req.getDescription(), req.getStartDate(),//
					req.getEndDate(), req.isPublished());
			if(updateRes!=1) {//表示資料沒更新成功
				return new BasicRes(ResCodeMessage.QUIZ_UPDATE_FAILED.getCode(), ResCodeMessage.QUIZ_UPDATE_FAILED.getMessage());
			}
			//刪除同一張問卷的所有問題
			questionDao.deleteByQuizId(quizId);
			//檢查問題
			List<QuestionVo> questionVoList = req.getQuestionList();
			for(QuestionVo vo : questionVoList) {
				checkRes=checkQuestionType(vo);
				if(checkRes!=null) {
					throw new Exception(checkRes.getMessage());
				}
				List<String> optionList=vo.getOptions();
				String str=mapper.writeValueAsString(optionList);
				questionDao.insert(quizId, vo.getQuestionId(), vo.getQuestion(), //
						vo.getType(), vo.isRequired(), str);
			}
		} catch (Exception e) {
			// 不能return BasicRes 而是要將發生的異常拋出去 這樣@Transaction 才會生效
			throw e;
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), ResCodeMessage.SUCCESS.getMessage());

	}

	@Override
	public QuestionRes getQuizByQuizId(int quizId) {
		if(quizId<=0) {
			return new QuestionRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		
		List<QuestionVo> questionVoList=new ArrayList<>();
		List<Question> list=questionDao.getQuestionsByQuizId(quizId);
		//把每題選項的資料型態從String 轉換成 List<String>
		for(Question item : list) {
			String str=item.getOptions();
			try {
				List<String> optionList = mapper.readValue(str, new TypeReference<>() {
				});
				//將從DB 取得的每一筆資料( Question item ) 的每個欄位值放到QuestionVo 中 以便返回給使用者
				//Question and QuestionVo 的差別在於選項 的資料型態
				QuestionVo vo=new QuestionVo(item.getQuizId(), item.getQuestionId(),//
						item.getQuestion(), item.getType(), item.isRequired(), optionList);
				//把每個vo放到questionVoList中
				questionVoList.add(vo);
			} catch (Exception e) {
				// 這邊不寫 throw e 是因為次方法中沒有使用 @Transactional，不影響返回結果
				return new QuestionRes(ResCodeMessage.OPTIONS_TRANSFER_ERROR.getCode(), //
						ResCodeMessage.OPTIONS_TRANSFER_ERROR.getMessage());
			}
		}
		return new QuestionRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), questionVoList);
	}
	

	@Override
	public SearchRes getAllQuizs() {
		List<Quiz> list=quizDao.getAll();
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
		//return null;
	}

	@Override
	public SearchRes search(SearchReq req) {
		//轉換req的值
		//若quizName 是null 轉成空字串
		
//		String quizName=req.getQuizName();
//		if(quizName==null) {
//			quizName="";
//		}else { //多餘的 不需要寫 但為了理解下面的3元運算子而寫
//			quizName=quizName;
//		}
		//3 元運算子
		//格式 變數名稱=條件判斷式 ? 判斷式結果為true時要賦予的值 : 判斷式結果為false時要賦予的值
		//quizName = quizName == null ? "" : quizName;
		//上面的程式碼可以只用下面一行來取得值
		String quizName=req.getQuizName() == null ? "" : req.getQuizName();
		//=================================
		//轉換開始時間 -->若沒有給開始日期 --> 給定一個很早的時間
		LocalDate startDate = req.getStartDate() == null ? LocalDate.of(1970, 1, 1) //
				: req.getStartDate();
		
		LocalDate endDate = req.getEndDate() == null ? LocalDate.of(2999, 12, 31) //
				: req.getEndDate();
		List<Quiz> list=new ArrayList<>();
		if(req.isPublished()) {
			list=quizDao.getAllPublished(quizName, startDate, endDate);
		}else {
			list=quizDao.getAll(quizName, startDate, endDate);
		}
		return new SearchRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), list);
		
	}
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public BasicRes delete(int quizId) throws Exception {
		if(quizId<=0) {
			return new BasicRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		try {
			quizDao.deleteById(quizId);
			questionDao.deleteByQuizId(quizId);
		} catch (Exception e) {
			throw e;
		}
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

}
