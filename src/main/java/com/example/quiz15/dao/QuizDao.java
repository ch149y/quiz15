package com.example.quiz15.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.entity.Quiz;

@Repository
public interface QuizDao extends JpaRepository<Quiz, Integer>{
	
	@Transactional
	@Modifying
	@Query(value="insert into quiz (name, description, start_date, end_date, is_published) "//
			+ " values (?1, ?2, ?3, ?4, ?5)", nativeQuery=true)
	public void insert(String name, String description, //
			LocalDate startDate, LocalDate endDate, boolean published);
	
	@Query(value="select max(id) from quiz ", nativeQuery=true)
	public int getMaxQuizId();
	
	@Query(value="select count(id) from quiz where id = ?1", nativeQuery=true)
	public int getCountByQuizId(int quizId);
	
	/**
	 * 回傳值資料型態設定成int 主要是用來判斷資料是否更新成功
	 * int =1 表示有資料被更新 0則表示無資料被更新
	 */
	@Transactional
	@Modifying
	@Query(value="update quiz set name=?2, description=?3, start_date=?4, "//
		+ " end_date=?5, is_published=?6 where id=?1", nativeQuery=true)
	public int update(int id, String name, String description, //
			LocalDate startDate, LocalDate endDate, boolean published);
	
	
	@Query(value="select * from quiz", nativeQuery=true)
	public List<Quiz> getAll();
	
	@Query(value="select * from quiz where like %?1% and start_date >= ?2 and end_date <= ?3", nativeQuery=true)
	public List<Quiz> getAll(String name, LocalDate startDate, LocalDate endDate);
	
}
