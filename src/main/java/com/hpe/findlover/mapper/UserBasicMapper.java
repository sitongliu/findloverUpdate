package com.hpe.findlover.mapper;

import com.hpe.findlover.model.Search;
import com.hpe.findlover.model.UserBasic;
import com.hpe.findlover.model.UserLabel;
import com.hpe.findlover.model.UserPick;
import com.hpe.util.BaseTkMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserBasicMapper extends BaseTkMapper<UserBasic> {
	UserBasic selectByEmail(String email);

	List<UserBasic> selectAllByIdentity(@Param("identity") String identity);

	/**
	 * 查询不在已经查出的列表中的人的基本信息
	 * @param id
	 * @param sexual
	 * @param point
	 * @param readyUserList
	 * @return
	 */
	List<UserBasic> findUserListByPointOther(@Param("id")Integer id, @Param("sexual")String sexual, @Param("point")String point, @Param("readyUserList")List<Integer> readyUserList);
	List<UserBasic> findUserListByPoint(@Param("id")Integer id, @Param("sexual")String sexual, @Param("point")String point);

	/**
	 * 查询符合用户性取向和地区的对应星级用户显示在广告位
	 * @return List<UserBasic> 返回的星级用户
	 **/
	List<UserBasic> selectStarUser(UserPick userPick);

	/**
	 * 通过用户性取向和用户所在地区查询用户
	 *
	 * @param sexual
	 * @param workProvince
	 * @return
	 */
	List<UserBasic> selectUserBySexualAndWorkProvince(@Param("id") Integer id,
	                                                  @Param("sexual") String sexual,
	                                                  @Param("workProvince") String workProvince);

	/**
	 * 通过搜索条件搜索用户
	 *
	 * @param search
	 * @return
	 */
	List<UserBasic> selectUserBySearch(Search search);

	/**
	 * 通过user-pick搜索用户
	 *
	 * @param userPick 用户择偶条件
	 * @return
	 */
	List<UserBasic> selectUserByUserPick(UserPick userPick);

	List<UserBasic> selectUsersByIds(Integer[] ids);

	//--------------------------------------后台功能------------------------------

	List<UserBasic> selectAllByIdentity(@Param("identity") String identity, @Param("column") String column, @Param("keyword") String keyword);
}