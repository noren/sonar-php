<?php

  echo (expression);                            // NOK {{Remove the parentheses from this "echo" call.}}
//^^^^

echo expression;                              // OK
echo (parenthesis_expression), expression;    // OK

