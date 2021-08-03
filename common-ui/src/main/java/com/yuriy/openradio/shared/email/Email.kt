/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.shared.email

import android.content.Context
import com.yuriy.openradio.shared.R
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import java.util.*
import javax.activation.CommandMap
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.activation.MailcapCommandMap
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Class to use when it needs to create and send email without Android's Email client by using javamail framework.
 */
class Email(context: Context) : Authenticator() {

    private var mUser = AppUtils.EMPTY_STRING
    private var mPwd = AppUtils.EMPTY_STRING
    private var mSubject = AppUtils.EMPTY_STRING
    private var mBody = AppUtils.EMPTY_STRING
    // smtp authentication - default on
    private val mAuth = true
    // debug mode on or off - default off
    private val mDebuggable = false
    private val mMultipart = MimeMultipart()

    init {
        mUser = context.resources.openRawResource(R.raw.email_usr).bufferedReader().use { it.readText() }
        mPwd = context.resources.openRawResource(R.raw.email_pwd).bufferedReader().use { it.readText() }
        // There is something wrong with MailCap, javamail can not find a
        // handler for the multipart/mixed part, so this bit needs to be added.
        val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
        CommandMap.setDefaultCommandMap(mc)
    }

    @Throws(Exception::class)
    fun send() {
        AppLogger.d("Sending email: subj:$mSubject, body:$mBody")
        val props = setProperties()
        val session = Session.getInstance(props, this)
        val msg = MimeMessage(session)
        msg.setFrom(InternetAddress(mUser))
        val addressTo = arrayOfNulls<InternetAddress>(1)
        addressTo[0] = InternetAddress(mUser)
        msg.setRecipients(MimeMessage.RecipientType.TO, addressTo)
        msg.subject = mSubject
        msg.sentDate = Date()

        // setup message body
        val messageBodyPart = MimeBodyPart()
        messageBodyPart.setText(mBody)
        mMultipart.addBodyPart(messageBodyPart)
        msg.setHeader("X-Priority", "1")
        // Put parts in message
        msg.setContent(mMultipart)

        // send email
        Transport.send(msg)
    }

    fun setBody(value: String) {
        mBody = value
    }

    fun setSubject(value: String) {
        mSubject = value
    }

    @Throws(Exception::class)
    fun addAttachment(filename: String) {
        val messageBodyPart = MimeBodyPart()
        val source = FileDataSource(filename)
        messageBodyPart.dataHandler = DataHandler(source)
        messageBodyPart.fileName = filename
        mMultipart.addBodyPart(messageBodyPart)
    }

    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(mUser, mPwd)
    }

    private fun setProperties(): Properties {
        // This function run once in a wild,
        // it is better to keep all strings as local variables to avoid heap pollution.
        val props = Properties()
        // Default smtp server
        props["mail.smtp.host"] = "smtp.gmail.com"
        if (mDebuggable) {
            props["mail.debug"] = "true"
        }
        if (mAuth) {
            props["mail.smtp.auth"] = "true"
        }
        // Default smtp port
        props["mail.smtp.port"] = "465"
        // Default socket factory port
        props["mail.smtp.socketFactory.port"] = "465"
        props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        props["mail.smtp.socketFactory.fallback"] = "false"
        return props
    }
}
