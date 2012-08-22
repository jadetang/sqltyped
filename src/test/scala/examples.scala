package sqltyped

import java.sql._
import org.scalatest._
import shapeless._

class ExampleSuite extends FunSuite with matchers.ShouldMatchers {
  Class.forName("com.mysql.jdbc.Driver")

  object Columns { object name; object age; object salary; object employer; object started
                   object resigned; object avg; object count }

  implicit val c = Configuration(Columns)
  implicit def conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sqltyped", "root", "")

  import Columns._

  test("Simple query") {
    val q = sql("select name, age from person")
    q().map(_.get(age)).sum should equal (50)
  }

  test("Query with input") {
    val q = sql("select name, age from person where age > ? order by name")
    q(30).map(_.get(name)) should equal (List("joe"))
    q(10).map(_.get(name)) should equal (List("joe", "moe"))

    val q2 = sql("select name, age from person where age > ? and name != ? order by name")
    q2(10, "joe").map(_.get(name)) should equal (List("moe"))
  }

  test("Query with join and column alias") {
    val q = sql("select p.name, j.name as employer, p.age from person p join job_history j on p.id=j.person order by employer")

    q().values should equal (List("joe" :: "Enron" :: 36 :: HNil, "joe" :: "IBM" :: 36 :: HNil))
    q().tuples should equal (List(("joe", "Enron", 36), ("joe", "IBM", 36)))
  }

  test("Query with optional column") {
    val q = sql("select p.name, j.name as employer, j.started, j.resigned from person p join job_history j on p.id=j.person order by employer")
    
    q().tuples should equal (List(
      ("joe", "Enron", date("2002-08-02 08:00:00.0"), Some(date("2004-06-22 18:00:00.0"))), 
      ("joe", "IBM",   date("2004-07-13 11:00:00.0"), None)))
  }

  test("Query with functions") {
    val q = sql("select avg(age), sum(salary) as salary, count(1) from person where abs(age) > ?")
    val res = q(10).head // FIXME .head is redundant, this query always returns just one row
    res.get(avg) should equal(Some(25.0))
    res.get(salary) should equal(Some(17500))
    res.get(count) should equal(2)

    val q2 = sql("select min(name) as name, max(age) as age from person where age > ?")
    val res2 = q2(10).head
    res2.get(name) should equal(Some("joe"))
    res2.get(age) should equal(Some(36))
    
    val res3 = q2(100).head
    res3.get(name) should equal(None)
    res3.get(age) should equal(None)
  }

  test("Query with just one selected column") {
    val q = sql("select name from person where age > ? order by name")
    q(10) should equal (List("joe", "moe"))    
  }
  
/*
  test("Query with constraint by unique column") {
    val q = sql("select age, name from person where id=?")
    q(1).tuples should equal (Some("joe", 36))
    
    val q2 = sql("select name from person where id=?")
    q2(1) should equal (Some("joe"))
    
    val q3 = sql("select name from person where id=? and age>?")
    q3(1, 20) should equal (Some("joe"))
    
    val q4 = sql("select name from person where id=? or age>?")
    q4(1, 20) should equal (List("joe"))
  }
  */

  def date(s: String) = 
    new java.sql.Timestamp(new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S").parse(s).getTime)
}
