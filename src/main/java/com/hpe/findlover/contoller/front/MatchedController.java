package com.hpe.findlover.contoller.front;

import com.hpe.findlover.model.UserBasic;
import com.hpe.findlover.service.MatchedService;
import com.hpe.findlover.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MatchedController {

    @Autowired
    MatchedService matchedService;

    @RequestMapping(value = "match")
    @ResponseBody
    public UserBasic match(HttpSession session, HttpServletResponse response) throws IOException {
        UserBasic user = SessionUtils.getSessionAttr("user",UserBasic.class);
        // 找到除了自身以外分数相接近的异性用户
        List<UserBasic> userList;
        // 已经匹配到的集合
        List<UserBasic> matchedUserList = (List<UserBasic>) SessionUtils.getSessionAttr("matchedUserList",ArrayList.class);
        if (matchedUserList == null) {
            userList = matchedService.findUserListByPoint(user);
        } else {
            userList = matchedService.findUserListByPointOther(user,matchedUserList);
        }
        UserBasic userDmin;

        if (userList.iterator().hasNext()) {
            userDmin = matchedService.KNNmatch(user, userList);
        } else {
            return new UserBasic();
        }
        session.setAttribute("matchedUser", userDmin);

        if (session.getAttribute("matchedUserList") == null) {
            matchedUserList = new ArrayList();
            matchedUserList.add(userDmin);
        } else {
            matchedUserList = (List<UserBasic>) session.getAttribute("matchedUserList");
            matchedUserList.add(userDmin);
        }
        session.setAttribute("matchedUserList", matchedUserList);
        return userDmin;
    }

}
