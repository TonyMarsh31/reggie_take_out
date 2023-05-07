package com.itheima.reggie.controller;

import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.common.UserInfo;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.exception.UnclassifiedBusinessException;
import com.itheima.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿管理
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {

    private final AddressBookService addressBookService;

    public AddressBookController(AddressBookService addressBookService) {
        this.addressBookService = addressBookService;
    }

    /**
     * 更新地址
     */
    @PutMapping
    public R<String> updateAdd(@RequestBody AddressBook addressBook) {
        if (addressBook == null) throw new UnclassifiedBusinessException("地址信息不存在");
        addressBookService.updateById(addressBook);
        return R.success("地址修改成功");
    }

    @DeleteMapping()
    public R<String> deleteAdd(@RequestParam("ids") Long id) {
        if (id == null || addressBookService.getById(id) == null) {
            throw new UnclassifiedBusinessException("地址信息不存在");
        }
        addressBookService.removeById(id);
        return R.success("地址删除成功");
    }

    /**
     * 新增收货地址
     */
    @PostMapping
    public R<AddressBook> save(@RequestBody AddressBook addressBook) {
        // 前端传递的dto缺少用户绑定，此处手动绑定用户ID
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    /**
     * 设置默认收货地址
     */
    @PutMapping("default")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook) {
        addressBookService.setDefaultAddressTo(addressBook);
        return R.success(addressBook);
    }

    /**
     * 根据id查询地址
     */
    @GetMapping("/{id}")
    public R<AddressBook> get(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        return addressBook != null ? R.success(addressBook) : R.error("没有找到该对象");
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    public R<AddressBook> getDefault() {
        AddressBook addressBook = addressBookService.getDefaultAddress(BaseContext.getCurrentId());
        return addressBook != null ? R.success(addressBook) : R.error("没有找到该对象");
    }

    @Autowired
    UserInfo userInfo;
    /**
     * 查询用户的全部地址
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list() {
//        List<AddressBook> result = addressBookService.getAllAddress(BaseContext.getCurrentId());
        List<AddressBook> result = addressBookService.getAllAddress(userInfo.getUserId());
        return R.success(result);
    }
}
