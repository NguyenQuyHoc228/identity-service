package com.devnguyen.identity_service.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/*
 * ConstraintValidator<A, T>:
 *   A = annotation type mà validator này phục vụ (DobConstraint)
 *   T = kiểu của field được validate (LocalDate)
 *
 * Spring sẽ tạo instance của class này và gọi:
 * 1. initialize(annotation) → đọc config từ annotation
 * 2. isValid(value, context) → thực hiện validate
 */
public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {

    /*
     * Lưu min từ annotation vào field để dùng trong isValid.
     * initialize() được gọi 1 lần sau khi object được tạo.
     */
    private int min;

    @Override
    public void initialize(DobConstraint constraintAnnotation) {
        /*
         * Đọc giá trị min từ annotation: @DobConstraint(min = 18) → min = 18
         * constraintAnnotation là instance của @DobConstraint tại field đó.
         */
        this.min = constraintAnnotation.min();
    }

    /*
     * isValid: logic validate thực sự.
     *
     * @param value: giá trị của field (LocalDate dob từ request)
     * @param context: context để tạo custom constraint violation message
     * @return true = hợp lệ, false = không hợp lệ
     *
     * Tại sao check Objects.isNull(value) trước?
     * → null handling nên tách biệt với logic validate.
     * → Nếu field null và bắt buộc → dùng @NotNull kết hợp.
     * → Validator chỉ validate khi có giá trị, tránh NullPointerException.
     * → Đây là convention của Jakarta Validation: validator nên bỏ qua null,
     *   để @NotNull chịu trách nhiệm check null.
     */
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) return true;

        /*
         * Tính số năm từ dob đến hôm nay.
         * ChronoUnit.YEARS.between(start, end): số năm đầy đủ giữa 2 ngày.
         * Ví dụ: sinh 2005-06-15, hôm nay 2024-01-01
         *   → between = 18 (chưa đủ 19 tuổi)
         *
         * Tại sao không dùng LocalDate.now().getYear() - dob.getYear()?
         * → Không chính xác! Sinh 2006-12-31, hôm nay 2024-01-01
         *   → 2024 - 2006 = 18 (nhưng thực ra chưa đến sinh nhật → chỉ 17 tuổi)
         * → ChronoUnit.YEARS.between tính chính xác số năm đầy đủ.
         */
        long years = ChronoUnit.YEARS.between(value, LocalDate.now());

        return years >= min;
    }
}