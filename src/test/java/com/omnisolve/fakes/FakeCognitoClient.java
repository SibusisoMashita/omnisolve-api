package com.omnisolve.fakes;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Fake {@link CognitoIdentityProviderClient} for integration tests.
 *
 * <p>Returns plausible stub responses without making any real AWS API calls.
 * Uses the same dynamic-proxy pattern as {@link FakeS3Client}.
 */
public class FakeCognitoClient {

    public static CognitoIdentityProviderClient create() {
        return (CognitoIdentityProviderClient) Proxy.newProxyInstance(
                CognitoIdentityProviderClient.class.getClassLoader(),
                new Class<?>[]{CognitoIdentityProviderClient.class},
                new CognitoInvocationHandler()
        );
    }

    private static class CognitoInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
            if ("equals".equals(methodName))   return proxy == args[0];
            if ("toString".equals(methodName)) return "FakeCognitoClient@" + Integer.toHexString(System.identityHashCode(proxy));
            if ("serviceName".equals(methodName)) return "cognito-idp";
            if ("close".equals(methodName))    return null;

            if ("adminCreateUser".equals(methodName) && args != null && args.length == 1
                    && args[0] instanceof AdminCreateUserRequest request) {
                return buildCreateUserResponse(request);
            }

            // adminEnableUser, adminDisableUser, adminAddUserToGroup — no-op stubs
            if (methodName.startsWith("admin")) {
                return null;
            }

            throw new UnsupportedOperationException(
                    "FakeCognitoClient: method not implemented: " + methodName);
        }

        private AdminCreateUserResponse buildCreateUserResponse(AdminCreateUserRequest request) {
            String fakeSub = UUID.randomUUID().toString();
            UserType user = UserType.builder()
                    .username(request.username())
                    .attributes(
                            AttributeType.builder().name("sub").value(fakeSub).build(),
                            AttributeType.builder().name("email").value(request.username()).build()
                    )
                    .build();
            return AdminCreateUserResponse.builder().user(user).build();
        }
    }
}
