package com.yunus.util;

import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public class PageableValidator {


    private PageableValidator() {}


    public static void validateSort(Pageable pageable, Set<String> allowedFields) {

        pageable.getSort().forEach(order -> {
            if (!allowedFields.contains(order.getProperty())){
                throw new BusinessException(ErrorType.VALIDATION_ERROR,"Geçersiz Sıralama...");
            }

        });

    }


}
