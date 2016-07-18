package com.example.neutrino.maze;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Created by Greg Stein on 7/18/2016.
 */
@Aspect
public class GlTraceAspect {

    private static final String POINTCUT_METHOD =
            "execution(@com.example.neutrino.maze.GlTrace * *(..))";

    private static final String POINTCUT_CONSTRUCTOR =
            "execution(@com.example.neutrino.maze.GlTrace *.new(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithGlTrace() {}

    @Pointcut(POINTCUT_CONSTRUCTOR)
    public void constructorAnnotatedWithGlTrace() {}

    @Around("methodAnnotatedWithGlTrace() || constructorAnnotatedWithGlTrace()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature signature = joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // Before method execution
        // -- nothing --

        Object result = joinPoint.proceed();

        // After method execution
        int errorCode = GLES20.glGetError();
        if (GLES20.GL_NO_ERROR != errorCode) {
            StringBuilder message = new StringBuilder();
            message.append("GlState[");
            message.append(methodName);
            message.append("]: ");
            message.append("ERROR:");
            message.append(GLU.gluErrorString(errorCode));
            Log.d(className, message.toString());
        }

        return result;
    }
}