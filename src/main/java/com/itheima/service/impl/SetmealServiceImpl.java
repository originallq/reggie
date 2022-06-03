package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.BusinessException;
import com.itheima.domain.Setmeal;
import com.itheima.domain.SetmealDish;
import com.itheima.dto.SetmealDto;
import com.itheima.mapper.SetmealMapper;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * @Description: 拓展方法, 存储套餐信息到setmeal表, 套餐菜品到setmeal_dish表
     * @Param: [setmealDto]
     * @Return: void
     * @Author: Ling
     */
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //存储套餐信息到setmeal表
        this.save(setmealDto);
        //存储套餐菜品到setmeal_dish表
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //数据中缺少setmealId,单独封装进去
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * @Description: 删除套餐，同时需要删除套餐和菜品的关联数据
     * @Param: [ids]
     * @Return: void
     * @Author: Ling
     */
    @Override
    public void deleteWithDish(List<Long> ids) {
        //select count(*) from setmeal where id in (1,2,3) and status = 1
        //查询套餐状态，确定是否可用删除
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.in(Setmeal::getId, ids);

        //1: 为处于售卖状态
        lqw.eq(Setmeal::getStatus, 1);
        int count = this.count(lqw);
        if (count > 0) {
            //不能删除,抛出出一个业务异常,展示提示信息
            throw new BusinessException("无法删除,有处于正在售卖中的套餐");
        }
        //可以正常删除,
        this.removeByIds(ids);

        //删除setmeal_dish表中的数据
        //delete from setmeal_dish where setmeal_id in (1,2,3)ids
        LambdaQueryWrapper<SetmealDish> sdLqw = new LambdaQueryWrapper<>();
        sdLqw.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(sdLqw);
    }

    /**
     * @Description: 根据id查询, 回显套餐信息与包含的菜品信息
     * @Param: [id]
     * @Return: java.util.List<com.itheima.dto.SetmealDto>
     * @Author: Ling
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        //1.查出setmeal表中的基本信息
        Setmeal setmeal = this.getById(id);

        //2.创建一个SetmealDto对象,通过拷贝赋值
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        //获取setmealId,然后获取对应的套餐菜品
        Long setmealId = setmeal.getId();
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId, setmealId);
        List<SetmealDish> list = setmealDishService.list(lqw);

        //封装到setmealDto中
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    /**
     * @Description: 修改套餐数据-->修改setmeal表和setmeal_dish表
     * @Param: [setmealDto]
     * @Return: void
     */
    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        //修改setmeal表信息
        this.updateById(setmealDto);

        //删除原先套餐中的菜品
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        //Long setmealDtoId = setmealDto.getId();
        lqw.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(lqw);

        //修改setmeal_dish表
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //封装setmeal_id字段
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }
}
