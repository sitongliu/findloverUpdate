package com.hpe.findlover.service;

import com.hpe.findlover.model.UserBasic;

import java.util.List;

public interface MatchedService extends BaseService<UserBasic>{
    void updatePoint(UserBasic user);

    List<UserBasic> findUserListByPoint(UserBasic user);

    UserBasic KNNmatch(UserBasic user, List<UserBasic> userList);

    void updateDeltaPoint(String username, Integer point);

    void updatePointAll();

    UserBasic findUserByUsername(String username);

    List<UserBasic> findUserListByPointOther(UserBasic user, List<UserBasic> matchedUserList);
}
