package com.example.tyrianflowbitequickstart.util

import tyrian.Cmd
import tyrian.cmds.*
import zio.*
import com.example.tyrianflowbitequickstart.until.HttpHelper
import com.example.tyrianflowbitequickstart.model.Msg
object Authentication {

  def authenticate(username: String, password: String): Cmd[Task, Msg] =
    HttpHelper.login(username, password)
}
