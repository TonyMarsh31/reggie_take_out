package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.AddressBook;

import java.util.List;

public interface AddressBookService extends IService<AddressBook> {

    AddressBook getDefaultAddress(Long userId);

    List<AddressBook> getAllAddress(Long userId);

    void setDefaultAddressTo(AddressBook addressBook);
}
