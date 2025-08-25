package com.example.quiz15.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz15.entity.User;

@Repository
public interface UserDao extends JpaRepository<User, String>{
	
	@Query(value="select count(email) from user where email=?1;", nativeQuery=true)
	public int getCountByEmail(String email);

	@Modifying
	@Transactional
	@Query(value="insert into user (name, phone, email, age, password) values " //
			+ " (?1, ?2, ?3, ?4, ?5) ;", //
			nativeQuery=true)
	public void addInfo(String name, String phone, String email, int age, String password);
	
	@Query(value="select password from user where email=?1;", nativeQuery=true)
	public String getPasswordByEmail(String email);
	
	@Query(value="select * from user where email=?1;", nativeQuery=true)
	public User getByEmail(String email);
}
