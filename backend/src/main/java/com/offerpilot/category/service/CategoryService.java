package com.offerpilot.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.offerpilot.category.dto.CategoryQuery;
import com.offerpilot.category.dto.CategoryUpsertRequest;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.vo.CategoryVO;
import java.util.List;

public interface CategoryService extends IService<Category> {
    List<CategoryVO> listCategories(CategoryQuery query);

    CategoryVO createCategory(CategoryUpsertRequest request);

    CategoryVO updateCategory(CategoryUpsertRequest request);

    void deleteCategory(Long id);

    Category getRequiredById(Long id);

    Category findOrCreateKnowledgeCategory(String name);
}
