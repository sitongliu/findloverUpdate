package com.hpe.findlover.service;

import com.hpe.findlover.model.Message;

import java.util.List;

public interface MessageService extends BaseService<Message>{
	List<Message> selectList() ;

	List<Message> selectMessageByColumn(String column,String keyword);

	List<Message> selectMessageByFollow(int userId);

}
