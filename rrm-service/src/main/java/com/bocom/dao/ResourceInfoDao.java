package com.bocom.dao;

import com.bocom.domain.ResourceInfo;
import com.bocom.dto.ResourceInfoDto;

import java.util.List;
import java.util.Map;

public interface ResourceInfoDao {
    int deleteByPrimaryKey(Integer id);

    int insert(ResourceInfo record);

    int insertSelective(ResourceInfo record);

    ResourceInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ResourceInfo record);

    int updateResourceInfo(ResourceInfo record);

    //查询资源信息
    List<ResourceInfoDto> queryResource(Map map);

    List<ResourceInfoDto> advancedSearch(Map map);

    List<ResourceInfoDto> queryServiceInfo(Map map);

    List<ResourceInfoDto> queryByResource(Map map);
}