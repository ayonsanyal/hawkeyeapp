package common

/**
  * Created by AYON SANYAL on 27-05-2018.
  * Mentions a result from a  service call, will be either a CompleteResult (Some), EmptyResult (None) or a Failure.
  *
  */
sealed trait ServiceResult[+A] {
  def map[B](f: A => B): ServiceResult[B] = EmptyResult

  def flatMap[B](f: A => ServiceResult[B]): ServiceResult[B] = EmptyResult
}

object ServiceResult {
  val UnexpectedFailure = ErrorMessage("common.unexpect", Some("An unexpected exception has occurred"))
}

/**
  * Full representation of a service call result.
  */
final case class CompleteResult[+A](value: A) extends ServiceResult[A] {
  override def map[B](f: A => B): ServiceResult[B] = CompleteResult(f(value))

  override def flatMap[B](f: A => ServiceResult[B]): ServiceResult[B] = f(value)
}

/**
  * This trait is defined to handle the empty and failure scenario for service call.
  */
sealed trait Empty extends ServiceResult[Nothing]


case object EmptyResult extends Empty

/**
  * Mentions an error message from a failure with a service call.  Consists of  fields for the code of the error
  * as well as a description of the error
  */
case class ErrorMessage(code: String, shortText: Option[String] = None)

/**
  * Describes a failure occurred from a call to a service with fields with failure type  as well as the error message
  * and optionally the exception
  */
sealed case class Failure(failType: FailureType.Value, message: ErrorMessage, exception: Option[Throwable] = None) extends Empty {
  type A = Nothing

  override def map[B](f: A => B): ServiceResult[B] = this

  override def flatMap[B](f: A => ServiceResult[B]): ServiceResult[B] = this
}

/**
  * Represents the type of failure encountered by the service
  */
object FailureType extends Enumeration {
  val Validation, Service = Value
}
