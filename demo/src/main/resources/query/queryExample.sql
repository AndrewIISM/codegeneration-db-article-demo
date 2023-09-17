select full_name
  from employees
 where salary > :minSalary
   and age > :minAge