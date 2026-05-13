package com.offerpilot.category.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.offerpilot.category.dto.CategoryQuery;
import com.offerpilot.category.dto.CategoryUpsertRequest;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.mapper.CategoryMapper;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.category.vo.CategoryVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.knowledge.entity.KnowledgeDoc;
import com.offerpilot.knowledge.mapper.KnowledgeDocMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final QuestionMapper questionMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;

    @Override
    public List<CategoryVO> listCategories(CategoryQuery query) {
        return lambdaQuery()
                .eq(StringUtils.hasText(query.getType()), Category::getType, query.getType())
                .orderByAsc(Category::getType, Category::getSortOrder, Category::getId)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CategoryVO createCategory(CategoryUpsertRequest request) {
        boolean exists = lambdaQuery()
                .eq(Category::getName, request.getName())
                .eq(Category::getType, request.getType())
                .exists();
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "category already exists");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setType(request.getType());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        save(category);
        return toVO(category);
    }

    @Override
    public CategoryVO updateCategory(CategoryUpsertRequest request) {
        Category category = getRequiredById(request.getId());
        boolean exists = lambdaQuery()
                .ne(Category::getId, request.getId())
                .eq(Category::getName, request.getName())
                .eq(Category::getType, request.getType())
                .exists();
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "category already exists");
        }
        category.setName(request.getName());
        category.setType(request.getType());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        updateById(category);
        return toVO(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = getRequiredById(id);
        Long questionCount = questionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                        .eq(Question::getCategoryId, id));
        Long docCount = knowledgeDocMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDoc>()
                        .eq(KnowledgeDoc::getCategoryId, id));
        if (questionCount > 0 || docCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "category is in use");
        }
        removeById(category.getId());
    }

    @Override
    public Category getRequiredById(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "category not found");
        }
        return category;
    }

    @Override
    public Category findOrCreateKnowledgeCategory(String name) {
        Category exists = lambdaQuery()
                .eq(Category::getName, name)
                .eq(Category::getType, "knowledge")
                .one();
        if (exists != null) {
            return exists;
        }
        Category category = new Category();
        category.setName(name);
        category.setType("knowledge");
        category.setSortOrder(0);
        category.setStatus(1);
        save(category);
        return category;
    }

    private CategoryVO toVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
    }
}
