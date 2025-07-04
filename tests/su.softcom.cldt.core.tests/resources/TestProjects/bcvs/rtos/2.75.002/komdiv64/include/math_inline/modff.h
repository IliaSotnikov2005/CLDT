/*
 * ============================
 * Copyright (C) 2008 NIISI RAN
 * ============================
 */
// Файл "modff.c" содержит функцию математической библиотеки.

// modff() - функция выделения целой и дробной части числа аргумента типа float.
//
// Синтаксис.
//   #include <math.h>
//   float modff(float x, float *iptr);
//
// Аргументы.
//   x    - переменная, целая и дробная часть которой будет вычислена;
//   iptr - указатель на переменную, куда будет записана целая часть числа x.
//
// Описание.
//   Функция раскладывает аргумент x на дробную часть f и целую часть i так,
//   что x = f + i, абсолютная величина f лежит в интервале [0, 1), i - целое число.
//   Оба числа f и i имеют тот же знак, что и аргумент x.
//
// Возвращаемое значение.
//   При успешном завершении функция возвращает значение дробной части аргумента x.
//   Переменной, на которую указывает аргумент pint,
//   присваивается значение целой частиаргумента x.
//
// Особые значения.
//   Если x равно NaN, то функция возвращает NaN.
//   Переменной, на которую указывает аргумент iptr, присваивается значение NaN.
//   Если x равно плюс или минус бесконечности, то функция возвращает ±0.
//   Переменной, на которую указывает аргумент iptr, присваивается значение аргумента x.
//
// Cтандарт.
//   Функция соответствует стандарту POSIX 1003.1, 2004 г.

#if( !defined( __MATH_INLINE_MODFF_H__ ))
#define __MATH_INLINE_MODFF_H__

#ifdef  BT_MATH_INLINE
#define modff MATH_INLINE_MODFF
#endif // BT_MATH_INLINE

#include <math_inline/truncf_fast.h>

static float __inline__ __UNUSED_F MATH_INLINE_MODFF( float x, float *iptr )
{
   FLT_INT_UNION X = { x };            // двоичное представление аргумента x со знаком
   FLT_INT_UNION ABS_X = { x };        // двоичное представление аргумента x без знака
   FLT_INT_UNION Y;

   FLT_INT_CLR_SIGN( ABS_X );
   if( FLT_INT_LT( ABS_X, FLT_INT_ONE ))
   {
      // |x| < 1, следовательно, целая часть равна 0
      (*iptr) = copysign( 0.0, x );    // целая часть равна 0
      return( x );                     // дробная часть равна x
   }
   Y.flt = MATH_INLINE_TRUNCF_FAST( x );// отбрасываем дробную часть - округление к нулю
   if( FLT_INT_EQ( X, Y ))
   {
      // х - целое число, или NaN, или -Inf, или +Inf
      (*iptr) = x;
      if( isnan( x ))
      {
         // х - не число (NaN)
         return( x );
      }
      // х - целое число
      return( copysign( 0.0, x ));     // дробная часть равна 0.0 со знаком x
   }
   (*iptr) = Y.flt;                    // целая числа х
   return( x - Y.flt );                // дробная часть числа х
}

#endif //__MATH_INLINE_MODFF_H__
