package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.mapper.AddressBookMapper;
import com.itheima.reggie.service.AddressBookService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {


    @Override
    public AddressBook getDefaultAddress(Long userId) {
        return lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, 1)
                .one();
    }

    @Override
    public List<AddressBook> getAllAddress(Long userId) {
        return lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .orderByDesc(AddressBook::getUpdateTime)
                .list();
    }

    @Override
    public void setDefaultAddressTo(AddressBook addressBook) {
        // 1.将当前用户的所有地址设置为非默认
        lambdaUpdate()
                .set(AddressBook::getIsDefault, 0)
                .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .update();
        // 2.将传递的地址设置为默认
        lambdaUpdate()
                .set(AddressBook::getIsDefault, 1)
                .eq(AddressBook::getId, addressBook.getId())
                .update();
    }
}
