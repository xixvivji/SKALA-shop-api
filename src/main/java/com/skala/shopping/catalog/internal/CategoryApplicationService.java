package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.CategoryView;
import com.skala.shopping.catalog.internal.domain.Category;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class CategoryApplicationService {
    private final CategoryRepository repository; private final Clock clock=Clock.systemUTC();
    public CategoryApplicationService(CategoryRepository repository){this.repository=repository;}
    @Transactional(readOnly=true) public List<CategoryView> getCategories(){return repository
            .findAllByStatusOrderByNameAscIdAsc(ProductStatus.ACTIVE).stream().map(Category::toView).toList();}
    @Transactional public CategoryView create(String name,String description){String normalized=normalize(name);
        unique(normalized);return repository.save(new Category(normalized,nullable(description),clock.instant())).toView();}
    @Transactional public CategoryView update(UUID id,String name,String description){Category category=find(id);
        String normalized=normalize(name);uniqueUnlessSame(category,normalized);category.update(normalized,nullable(description),clock.instant());return category.toView();}
    @Transactional public void delete(UUID id){find(id).delete(clock.instant());}
    private Category find(UUID id){return repository.findByIdAndStatusNot(id,ProductStatus.DELETED)
            .orElseThrow(()->new BusinessException(ErrorCode.DATA_NOT_FOUND,"카테고리를 찾을 수 없습니다."));}
    private void unique(String name){if(repository.existsByNameIgnoreCaseAndStatusNot(name,ProductStatus.DELETED))
        throw new BusinessException(ErrorCode.DATA_DUPLICATED,"같은 카테고리명이 있습니다.");}
    private void uniqueUnlessSame(Category category,String name){if(!category.toView().getName().equalsIgnoreCase(name))unique(name);}
    private String normalize(String value){return value.trim();} private String nullable(String value){return value==null||value.isBlank()?null:value.trim();}
}
