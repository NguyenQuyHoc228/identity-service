package com.devnguyen.identity_service.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/*
 * Custom Constraint Annotation.
 *
 * Cách hoạt động:
 * 1. Bạn đặt @DobConstraint(min=18) lên field dob
 * 2. Khi @Valid được trigger, Spring tìm @Constraint(validatedBy = DobValidator.class)
 * 3. Spring tạo instance DobValidator và gọi isValid(fieldValue, context)
 * 4. isValid trả true/false → pass hoặc fail validation
 *
 * @Target: annotation này có thể đặt ở đâu?
 *   FIELD           → trên field của class
 *   METHOD          → trên method return value
 *   PARAMETER       → trên parameter của method
 *   ANNOTATION_TYPE → có thể compose annotation khác (meta-annotation)
 *
 * @Retention(RUNTIME): annotation phải tồn tại lúc runtime
 *   vì Spring dùng reflection để đọc annotation lúc chạy.
 *   (Nếu là SOURCE hoặc CLASS → mất lúc compile hoặc không load được lúc runtime)
 *
 * @Constraint(validatedBy = DobValidator.class):
 *   Khai báo class nào sẽ thực hiện validation logic.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {DobValidator.class})
public @interface DobConstraint {

    /*
     * 3 attribute BẮT BUỘC của mọi constraint annotation (Jakarta Validation spec):
     *
     * message(): error message khi validation fail.
     *   Default value ở đây là "" → caller phải cung cấp message.
     *   Mình dùng message như ErrorCode key: message = "INVALID_DOB"
     *
     * groups(): cho phép group các constraint lại, apply trong những tình huống khác nhau.
     *   Ví dụ: group A cho create, group B cho update → validate rule khác nhau.
     *   Ít dùng trong project nhỏ, nhưng bắt buộc phải có theo spec.
     *
     * payload(): metadata gắn thêm vào constraint, thường dùng để phân loại severity.
     *   Cũng ít dùng nhưng bắt buộc theo spec.
     */
    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /*
     * Custom attribute: tuổi tối thiểu.
     * Caller dùng: @DobConstraint(min = 18)
     * Validator đọc: context.getConstraintDescriptor().getAttributes().get("min")
     */
    int min();
}