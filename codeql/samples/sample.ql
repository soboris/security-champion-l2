/**
 * @name Simple XSS Detection
 * @description Find template literals with variables that could be XSS
 * @kind problem
 * @problem.severity warning
 * @id js/demo-xss
 * @tags security
 */

import javascript

from TemplateLiteral template, VarAccess var
where 
  template.getAnElement() = var and
  (var.getName() = "query" or var.getName() = "name")
select template, "XSS vulnerability at line " + template.getLocation().getStartLine() + ": Template literal contains unescaped variable '" + var.getName() + "'"
