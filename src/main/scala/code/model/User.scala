package code.model

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.record.field.TextareaField
import scala.xml.Text
import net.liftweb.util.Helpers._
import com.foursquare.rogue.Rogue._
import net.liftweb.mongodb.record.MongoMetaRecord
import net.liftweb.mongodb.record.field.ObjectIdPk
import net.liftweb.mongodb.record.MongoRecord
import net.liftweb.json.JsonDSL._
import net.liftweb.record.MetaMegaProtoUser
import net.liftweb.record.MegaProtoUser
import scala.xml.Elem
import net.liftweb.http.S
import scala.xml.NodeSeq
import net.liftweb.proto.ProtoUser
import net.liftweb.record.field.LongField
import net.liftweb.http.SHtml
import net.liftweb.http.S.LFuncHolder

/**
 * The singleton that has methods for accessing the database
 */
object User extends User with  MetaMegaProtoUser[User] with MongoMetaRecord[User] {

  override def screenWrap = Full(<lift:surround with="default" at="content"><lift:bind /></lift:surround>)

  // define the order fields will appear in forms and output
  override def fieldOrder = List(firstName, lastName, email, locale, timezone, password)

  // comment this line out to require email validations
  override def skipEmailValidation = true

  protected def userFromStringId(id: String): Box[User] =
    try { User where (_.id eqs id.toLong) get } catch { case _ => Empty }

  protected def findUserByUniqueId(id: String): Box[User] =
    try { (User where (_.id eqs id.toLong) fetch(1)).headOption } catch { case _ => Empty }

  /**
   * Given an username (probably email address), find the user
   */
  protected def findUserByEmail(email: String): Box[User] = (User where (_.email eqs email) fetch(1)).headOption

  protected def findUserByUserName(email: String): Box[User] = findUserByEmail(email)


  override def changePassword = {
    val user = currentUser.open_! // we can do this because the logged in test has happened
    var oldPassword = ""
    var newPassword: List[String] = Nil

    def testAndSet() {
      if (!user.testPassword(Full(oldPassword))) S.error(S.??("wrong.old.password"))
      else {
        user.setPasswordFromListString(newPassword)
        user.validate match {
          case Nil => user.save; S.notice(S.??("password.changed")); S.redirectTo(homePage)
          case xs => println("Errors validating: "+xs); S.error(xs)
        }
      }
    }

    bind("user", changePasswordXhtml,
         "old_pwd" -> SHtml.password("", s => oldPassword = s),
         "new_pwd" -> SHtml.password_*("", LFuncHolder(s => newPassword = s)),
         "submit" -> changePasswordSubmitButton(S.??("change"), testAndSet _))
  }

}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User private() extends MegaProtoUser[User] with MongoRecord[User] {

  def meta = User

  // define an additional field for a personal essay
  object textArea extends TextareaField(this, 2048) {
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = "Personal Essay"
  }

  /**
   * Make sure that the email field is unique in the database
   */
  override def valUnique(msg: => String)(email: String): List[FieldError] = {
    if ((User where (_.email eqs email) and (_.id neqs id.is) get ()).isDefined)
      List(FieldError(this.email, Text(msg))) else Nil
  }

  /**
   * The original ProtoUser implementation sets this always to 0.
   * A better strategy to ensure uniqueness is an own collection in which
   * a value is increased as in a sequence. Hint: Mongo's findAndModify
   * can be used for atomically reading a value and increasing it ensuring that
   * concurrent access will not return the same result.
   */
  override lazy val id = new MyMappedLongClass(this)

  protected class MyMappedLongClass(obj: User) extends LongField(obj) {
    override def defaultValue = randomLong(java.lang.Long.MAX_VALUE)
  }

  override lazy val password = new MyPassword(this) {

    protected def appendFieldId(in: Elem): Elem = uniqueFieldId match {
      case Full(i) => {
        import net.liftweb.util.Helpers._
        in % ("id" -> i)
      }
      case _ => in
    }

    // S.SFuncHolder(this.setPassword(_))
    private def elem2 = S.fmapFunc({pwd: List[String] => pwd match {
      case x1 :: x2 :: Nil if x1 == x2 => this(x1)
      case _ => Nil
      }}) { funcName => <span>
                        {appendFieldId(<input type="password" name={funcName} value="" tabindex={tabIndex toString}/>)}
                        &nbsp;{S.??("repeat")}&nbsp;
                        <input type="password" name={funcName} value="" tabindex={tabIndex toString}/>
                      </span>
    }

    override def toForm: Box[NodeSeq] = Full(elem2)

  }

  def setPasswordFromListString(pwd: List[String]): User.TheUserType = {
    pwd match {
      case x1 :: x2 :: Nil if x1 == x2 => println("text is the same. Apply it"); password.setPlain(x1)
      case _ => println("Text's are not the same. Not applying"); Nil
    }
    //this.password.setFromAny(pwd)
    this
  }

}

