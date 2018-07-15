package com.hpe.findlover.service.impl;

import com.hpe.findlover.mapper.UserBasicMapper;
import com.hpe.findlover.model.UserBasic;
import com.hpe.findlover.service.MatchedService;
import com.hpe.util.BaseTkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchedServiceImpl extends BaseServiceImpl<UserBasic> implements MatchedService {

    private Logger logger= LoggerFactory.getLogger(this.getClass());


    private final UserBasicMapper userBasicMapper;

    @Autowired
    public MatchedServiceImpl(UserBasicMapper userBasicMapper) {
        this.userBasicMapper = userBasicMapper;
    }


    @Override
    public BaseTkMapper<UserBasic> getMapper() {
        return userBasicMapper;
    }
    /**
     * 根据条件更新用户的评分
     * @param user
     */
    @Override
    public void updatePoint(UserBasic user) {
        String edu = user.getEducation();   // 教育水平
        Integer age = user.getAge();        // 年龄
        Double height = (double)user.getHeight(); // 身高
        Double income = user.getSalary();  // 收入
        String gender = user.getSex();    // 性别

        Double Point,Pe,Pa,Ph,Pi;

        //Pe 计算算法:
        switch (edu) {
            case "大学本科":
                Pe = 0.125;
                break;
            case "硕士":
                Pe = 0.1875;
                break;
            case "博士":
                Pe = 0.25;
                break;
            case "其他":
                Pe = 0.0625;
                break;
            default:
                Pe = 0.0;
        }

        //Pa计算算法:
        if(age >= 18 && age <=23){
            Pa = 0.05*age-0.9;
        }else if(age > 23 && age <= 60){
            Pa = (-0.000183*age*age) + (0.008418*age) + 0.15372;
        }else{
            Pa = 0.0;
        }


        //Pi计算算法:
        income /= 1000;
        if(income >= 3){
            Pi = 1/(2*(1-income)) + 0.25;
        }else{
            Pi = 0.0;
        }


        //Ph计算算法(男女不同)
        if(gender.equals("男")){
            Double dheight = (double) (height / 100);
            Ph = -4*dheight*dheight + 14.8*dheight -13.44;
        }else{
            Double dheight = (double) (height / 100);
            Ph = -2.78*dheight*dheight + 10*dheight -8.75;
        }

        //Point总分统计(扩大1000倍):总分1000,随着评价动态上升;
        Point = (Pa + Pe + Ph + Pi) * 1000;
        Integer point = (int) Math.round(Point);

        //更新用户评分
        user.setPoint(point.toString());

        userBasicMapper.updateByPrimaryKeySelective(user);

    }

    @Override
    public List<UserBasic> findUserListByPoint(UserBasic user) {

        String sexual = user.getSexual();
        String point = user.getPoint();
        Integer id = user.getId();

        return userBasicMapper.findUserListByPoint(id,sexual,point);
    }

    /**
     * 在查找出来的UserList中寻找最优的匹配者
     * @param user
     * @param userList
     * @return
     */
    @Override
    public UserBasic KNNmatch(UserBasic user, List<UserBasic> userList) {

        Integer ageTempMax = 0;
        Double heightTempMax = 0.0;
        Double incomeTempMax = 0.0;

        Integer ageTempMin = 150;
        Double heightTempMin = 3.0;
        Double incomeTempMin = 1.0;
        //获取:userList中age,income,height,edu的最大值与最小值
        for(int i = 0;i < userList.size();i++){
            UserBasic otherUser = userList.get(i);
            Integer age = otherUser.getAge();
            Double height = ((double)otherUser.getHeight())/100.0;
            Double income = otherUser.getSalary()/1000.0;
            if(age > ageTempMax){
                ageTempMax = age;
            }
            if(height > heightTempMax){
                heightTempMax = height;
            }
            if(income > incomeTempMax){
                incomeTempMax = income;
            }
            if(age < ageTempMin){
                ageTempMin = age;
            }
            if(height < heightTempMin){
                heightTempMin = height;
            }
            if(income < incomeTempMin){
                incomeTempMin = income;
            }
        }

        UserBasic user1 = new UserBasic();
        user1.setSex(user.getSex());
        user1.setAge(user.getAge());;
        user1.setSalary(user.getSalary());
        user1.setEducation(user.getEducation());
        user1.setHeight(user.getHeight());

        //根据最大最小值将集合与用户数据归一化
        Integer userage = user1.getAge();
        if((int)(100*(double)(userage-ageTempMin)/(double)(ageTempMax-ageTempMin))>=0){
            user1.setAge((int)(100*(double)(userage-ageTempMin)/(double)(ageTempMax-ageTempMin)));
        }else{
            user1.setAge(0);
        }
        Double userincome = user1.getSalary()/1000.0;
        if((userincome-incomeTempMin)/(incomeTempMax-incomeTempMin)>=0){
            user1.setSalary((userincome-incomeTempMin)/(incomeTempMax-incomeTempMin));
        }else{
            user1.setSalary(0.0);
        }
        Double userheight = ((double)user1.getHeight())/100.0;
        if((double)(userheight-heightTempMin)/(double)(heightTempMax-heightTempMin)>=0){
            user1.setHeight((int) ((userheight-heightTempMin)/(heightTempMax-heightTempMin)));
        }else{
            user1.setHeight(0);
        }
        for(UserBasic otheruser : userList){
            Integer otherage = otheruser.getAge();
            otheruser.setAge((int)(100*(double)(otherage-ageTempMin)/(double)(ageTempMax-ageTempMin)));
            Double otherincome = otheruser.getSalary()/1000.0;
            otheruser.setSalary((otherincome-incomeTempMin)/(incomeTempMax-incomeTempMin+1));
            Double otherheight = ((double)otheruser.getHeight())/100.0;
            otheruser.setHeight((int) ((otherheight-heightTempMin)/(heightTempMax-heightTempMin)));
        }

        Double dTemp = 10000.0;
        Integer duserage1 = user1.getAge();
        Double duserage = (double)duserage1/100.0;
        Double duserheight = (double)user1.getHeight();
        Double duserincome = user1.getSalary();
        Map<Double,Integer> dmap = new HashMap<Double,Integer>();
         for (int i = 0; i < userList.size(); i++) {
            UserBasic otheruser = userList.get(i);
            Integer dotherage1 = otheruser.getAge();
            Double dotherage = (double)dotherage1/100.0;
            Double dotherheight = Double.valueOf(otheruser.getHeight());
            Double dotherincome = otheruser.getSalary();
            Double dNow = Math.sqrt(2*Math.abs(dotherage*dotherage-duserage*duserage))
                    + Math.sqrt(Math.abs(dotherheight*dotherheight-duserheight*duserheight))
                    + Math.sqrt(Math.abs(dotherincome*dotherincome-duserincome*duserincome));
            //将所有距离存入map中封装,为后续匹配多个用户做准备
            dmap.put(dNow, i);
            if(dTemp > dNow){
                dTemp = dNow;
            }
        }
        //计算最优相似度
        Double similarity = 1/dTemp;
        System.out.println("两个用户的相似度为"+similarity);
        UserBasic userdmin = userList.get(dmap.get(dTemp));
        //由于userList数据被归一化一次了,故要根据用户名重新返回user对象.
        String userEmail=userdmin.getEmail();
      //  Integer userdminId=userdmin.getId();
        Integer userdminAge = userdmin.getAge();

        // 查询出用户所有的信息
       // UserBasic usersimiler = userBasicMapper.selectByPrimaryKey(userdminId);
        UserBasic usersimiler = userBasicMapper.selectByEmail(userEmail);
        usersimiler.setAge(userdminAge);

        // 返回所有的信息
        return usersimiler;
    }

    @Override
    public void updateDeltaPoint(String username, Integer point) {

    }

    @Override
    public void updatePointAll() {

    }

    @Override
    public UserBasic findUserByUsername(String username) {
        return null;
    }

    @Override
    public List<UserBasic> findUserListByPointOther(UserBasic user, List<UserBasic> matchedUserList) {
        String sexual = user.getSexual();
        String point = user.getPoint();
        Integer id = user.getId();
        List<Integer> readyUserList = new ArrayList<Integer>();
        for (UserBasic user2 : matchedUserList) {
            readyUserList.add(user2.getId());
        }


        return userBasicMapper.findUserListByPointOther(id, sexual, point,readyUserList);




    }


}
