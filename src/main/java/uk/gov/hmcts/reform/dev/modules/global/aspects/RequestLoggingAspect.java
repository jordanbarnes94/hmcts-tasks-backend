package uk.gov.hmcts.reform.dev.modules.global.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingAspect.class);

    /**
     * Intercepts all controller methods and logs @RequestBody parameters.
     * Note: this only runs for requests that pass validation. Requests that fail
     * validation are logged by GlobalExceptionHandler.handleValidationErrors instead.
     */
    @Before("execution(* uk.gov.hmcts.reform.dev.modules.tasks.controllers..*(..))")
    public void logRequestBody(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        // Find and log @RequestBody parameters
        for (int i = 0; i < parameters.length; i++) {
            if (hasRequestBodyAnnotation(parameters[i])) {
                String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
                logger.info("Request to {}: {}", methodName, formatRequestBody(args[i]));
            }
        }
    }

    private boolean hasRequestBodyAnnotation(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
            .anyMatch(annotation -> annotation.annotationType().equals(RequestBody.class));
    }

    private String formatRequestBody(Object requestBody) {
        if (requestBody == null) {
            return "null";
        }

        // Use toString() which Lombok @Getter classes provide a reasonable implementation for
        // For more complex formatting, you could use Jackson's ObjectMapper here
        return requestBody.toString();
    }
}
