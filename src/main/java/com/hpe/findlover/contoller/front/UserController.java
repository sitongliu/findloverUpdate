package com.hpe.findlover.contoller.front;

import com.hpe.findlover.model.UserBasic;
import com.hpe.findlover.model.UserPick;
import com.hpe.findlover.service.LabelService;
import com.hpe.findlover.service.UserLabelService;
import com.hpe.findlover.service.UserPickService;
import com.hpe.findlover.service.UserService;
import com.hpe.findlover.token.CustomToken;
import com.hpe.findlover.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class UserController {
	private Logger logger = LogManager.getLogger(UserController.class);

	@Autowired
    UserService userService;
	@Autowired
    UserPickService userPickService;
	@Autowired
    UserLabelService userLabelService;
	@Autowired
    LabelService labelService;


	@GetMapping("login")
	public String login() {
		return "front/login";
	}
	@GetMapping("logout")
	public String logout(){
		SecurityUtils.getSubject().logout();
		SecurityUtils.getSubject().getSession().removeAttribute("user");
		return "redirect:login";
	}

	@GetMapping("register")
	public String register(){
		return "front/register";
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(sdf, true));
	}

	@PostMapping("register")
	public String register(UserBasic user,RedirectAttributes redirectAttributes,HttpServletRequest request)throws  Exception{
		//目前先不加上后台验证
//		将居住地的数据进行拼接
		String province=request.getParameter("province");
		String city=request.getParameter("city");
		user.setWorkplace(province+"-"+city);
//		设置注册页面没有的必填信息
        String uuid = UUID.randomUUID().toString();
        user.setCode(uuid);
		user.setPassword(new Md5Hash(user.getPassword(), ByteSource.Util.bytes(user.getEmail())).toString());
        user.setAuthority(1);
        //暂时将状态码设置为1
        user.setStatus(1);
        user.setPhoto("男".equals(user.getSex()) ? Constant.MALE_PHOTO : Constant.FEMALE_PHOTO);
        user.setRegTime(new Date());
		HttpSession session = request.getSession();
		/**
		 * todo
 		 */
        user.setPoint("564");
//		发送邮件
//		String url= LoverUtil.getBasePath(request)+"/"+"active?email="+user.getEmail()+"&code="+uuid;
        //	EmailUtil.sendEmailByWeb(user.getEmail(),url);
        //将用户存放在数据库中
        if (userService.insertUseGeneratedKeys(user)>0) {
            //添加高收入，高学历标签
            userService.updateUserBasicAndUserLabel(user);
            UserBasic userBasic = userService.selectByEmail(user.getEmail());
            //用户注册成功之后，生成默认的择偶条件和标签信息
            UserPick userPick = new UserPick();
            userPick.setId(userBasic.getId());
            userPick.setSex(userBasic.getSexual());
            userPick.setAgeLow(Math.max(18, LoverUtil.getAge(userBasic.getBirthday()) - 3));
            userPick.setAgeHigh(Math.min(66, LoverUtil.getAge(userBasic.getBirthday()) + 3));
            userPick.setWorkplace(userBasic.getWorkplace());
            userPick.setMarryStatus("未婚");
            userPick.setHeightLow(Math.max(145, userBasic.getHeight() - 10));
            userPick.setHeightHigh(Math.min(210, userBasic.getHeight() + 10));
            if (userPickService.insert(userPick)) {
                return "redirect:login";
            } else {
                return "redirect:register";
            }

        } else {
            return "redirect:register";
        }
    }

    @PutMapping("user")
	@ResponseBody
	public boolean updateUser(UserBasic userBasic,HttpServletRequest request){
		userBasic.setId(SessionUtils.getSessionAttr("user",UserBasic.class).getId());
		if(userService.updateByPrimaryKeySelective(userBasic)){
			request.getSession().setAttribute("user",userService.selectByPrimaryKey(userBasic.getId()));
			return true;
		}
		return false;
	}

	@RequestMapping("checkEmail")
	@ResponseBody
	public String checkEmail(@RequestParam("email")String email){
		UserBasic userBasic = userService.selectByEmail(email);
		if (userBasic!=null){
			return "{\"error\":\"该邮箱已被注册！\"}";
		}else {
			return "{\"ok\":\"此邮箱可用！\"}";
		}
	}

	@RequestMapping("checkid")
	@ResponseBody
	public String checkid(@RequestParam("otherId")int otherId, HttpSession session){
		UserBasic userBasic = userService.selectByPrimaryKey(otherId);
		UserBasic user= (UserBasic) session.getAttribute("user");
		if (user.getId().equals(otherId)){
			return "{\"error\":\"您不能和自己搞对象！\"}";
		}else if(userBasic!=null){
			return "{\"ok\":\"我们将会请ta验证该消息~\"}";
		}else {
			return "{\"error\":\"该id不存在！\"}";
		}
	}

	@GetMapping("user/exists/{id}")
	@ResponseBody
	public boolean existsById(@PathVariable int id){
		UserBasic basic = new UserBasic();
		basic.setId(id);
		return userService.selectOne(basic) != null;
	}

	@PostMapping("login")
	public String login(HttpServletRequest request,UserBasic user,boolean rememberMe, RedirectAttributes redirectAttributes) {
		if (StringUtils.isEmpty(user.getEmail()) || StringUtils.isEmpty(user.getPassword())) {
			redirectAttributes.addAttribute("message", "用户名或密码不能为空！");
			return "redirect:login";
		}
		CustomToken token = new CustomToken(user.getEmail(), user.getPassword(), Identity.USER);
		logger.info("rememberMe: " + rememberMe);
		token.setRememberMe(rememberMe);
		try {
			SecurityUtils.getSubject().login(token);
			if (SecurityUtils.getSubject().isAuthenticated()) {
				ShiroHelper.flushSession();
				HttpSession session = request.getSession();
				UserBasic userBasic = userService.selectByEmail(user.getEmail());
				userService.userAttrHandler(userBasic);
				session.setAttribute("user", userBasic);
				return "redirect:index";
			}
			else{
				return "redirect:login";
			}
		} catch (UnknownAccountException uae) {
			logger.debug("对用户[" + user.getEmail() + "]进行登录验证..验证未通过,未知账户");
			redirectAttributes.addAttribute("message", "用户名不存在");
		} catch (IncorrectCredentialsException ice) {
			logger.debug("对用户[" + user.getEmail() + "]进行登录验证..验证未通过,错误的凭证");
			redirectAttributes.addAttribute("message", "密码不正确");
		} catch (LockedAccountException ule){
			logger.debug("对用户[" + user.getEmail() + "]进行登录验证..验证未通过,用户被锁定");
			redirectAttributes.addAttribute("message", "用户被锁定");
		}catch (DisabledAccountException dae){
			logger.debug("对用户[" + user.getEmail() + "]进行登录验证..验证未通过,用户未激活");
			redirectAttributes.addAttribute("message", "用户未激活");
		}
		return "redirect:login";
	}
	@PostMapping("getUserById")
	@ResponseBody
	public UserBasic getById(int otherUserId){
		logger.debug("otherUserId=="+otherUserId);
		return userService.selectByPrimaryKey(otherUserId);
	}
	@GetMapping("session/user")
	@ResponseBody
	public UserBasic getSessionUser(HttpServletRequest request){
		return SessionUtils.getSessionAttr("user", UserBasic.class);
	}

}