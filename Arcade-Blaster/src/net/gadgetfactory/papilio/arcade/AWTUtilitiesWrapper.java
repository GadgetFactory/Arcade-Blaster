/*
  Part of the Papilio Arcade Blaster

  Copyright (c) 2010-12 GadgetFactory LLC

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.gadgetfactory.papilio.arcade;

import java.awt.GraphicsConfiguration;
import java.awt.Shape;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AWTUtilitiesWrapper
{
    private static Class<?> awtUtilitiesClass;
    private static Class<?> translucencyClass;
    private static Method mth_IsTranslucencySupported,  mth_IsTranslucencyCapable;
    private static Method mth_SetWindowShape,  mth_SetWindowOpacity,  mth_SetWindowOpaque;
    public static Object PERPIXEL_TRANSPARENT,  TRANSLUCENT,  PERPIXEL_TRANSLUCENT;

    static {
        try {
            awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
            translucencyClass = Class.forName("com.sun.awt.AWTUtilities$Translucency");
            if (translucencyClass.isEnum()) {
                Object[] kinds = translucencyClass.getEnumConstants();
                if (kinds != null) {
                    PERPIXEL_TRANSPARENT = kinds[0];
                    TRANSLUCENT = kinds[1];
                    PERPIXEL_TRANSLUCENT = kinds[2];
                }
            }
            mth_IsTranslucencySupported = awtUtilitiesClass.getMethod("isTranslucencySupported", translucencyClass);
            mth_IsTranslucencyCapable = awtUtilitiesClass.getMethod("isTranslucencyCapable", GraphicsConfiguration.class);
            mth_SetWindowShape = awtUtilitiesClass.getMethod("setWindowShape", Window.class, Shape.class);
            mth_SetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity", Window.class, float.class);
            mth_SetWindowOpaque = awtUtilitiesClass.getMethod("setWindowOpaque", Window.class, boolean.class);
        } catch (NoSuchMethodException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (SecurityException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (ClassNotFoundException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        }
    }

    private static boolean isSupported(Method method, Object kind) {
        if (awtUtilitiesClass == null || method == null) {
            return false;
        }
        try {
            Object ret = method.invoke(null, kind);
            if (ret instanceof Boolean) {
                return ((Boolean)ret).booleanValue();
            }
        } catch (IllegalAccessException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (IllegalArgumentException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (InvocationTargetException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        }
        return false;
    }
    
    public static boolean isTranslucencySupported(Object kind) {
        if (translucencyClass == null) {
            return false;
        }
        return isSupported(mth_IsTranslucencySupported, kind);
    }
    
    public static boolean isTranslucencyCapable(GraphicsConfiguration gc) {
        return isSupported(mth_IsTranslucencyCapable, gc);
    }
    
    private static void set(Method method, Window window, Object value) {
        if (awtUtilitiesClass == null ||
                method == null)
        {
            return;
        }
        try {
            method.invoke(null, window, value);
        } catch (IllegalAccessException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (IllegalArgumentException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        } catch (InvocationTargetException ex) {
            System.err.println(AWTUtilitiesWrapper.class.getName() + " : " + ex);
        }
    }
    
    public static void setWindowShape(Window window, Shape shape) {
        set(mth_SetWindowShape, window, shape);
    }

    public static void setWindowOpacity(Window window, float opacity) {
        set(mth_SetWindowOpacity, window, Float.valueOf(opacity));
    }
    
    public static void setWindowOpaque(Window window, boolean opaque) {
        set(mth_SetWindowOpaque, window, Boolean.valueOf(opaque));
    }
}
