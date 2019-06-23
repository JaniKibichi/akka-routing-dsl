package com.credomob.utils

import com.typesafe.config.ConfigFactory

object VendorPointConfig{
  private val config = ConfigFactory.load()
  private val vendorPointConfig = config.getConfig("vendorPoint")

  object http{
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }

  object general{
    private val general = vendorPointConfig.getConfig("general")
    val responseUri = general.getString("responseUri")
    val balanceEndpoint = general.getString("balanceEndpoint")
  }

  object auth{
    private val auth = vendorPointConfig.getConfig("auth")
    val responseUri = auth.getString("responseUri")
    val loginEndpoint = auth.getString("loginEndpoint")
  }

  object pinless{
    private val pinless = vendorPointConfig.getConfig("pinless")
    val responseUri = pinless.getString("responseUri")
    val buyPinlessEndpoint = pinless.getString("buyPinlessEndpoint")
    val checkPinlessTransactionEndpoint = pinless.getString("checkPinlessTransactionEndpoint")
  }

  object pin{
    private val pin = vendorPointConfig.getConfig("pin")
    val responseUri = pin.getString("responseUri")
    val buyEndpoint = pin.getString("buyEndpoint")
    val checkTransactionEndpoint = pin.getString("checkTransactionEndpoint")
  }

  object postpaid{
    private val postpaid = vendorPointConfig.getConfig("postpaid")
    val responseUri = postpaid.getString("responseUri")
    val postpaidverifyEndpoint = postpaid.getString("postpaidverifyEndpoint")
    val postpaidbillEndpoint = postpaid.getString("postpaidbillEndpoint")
    val checkPostPaidTransactionEndpoint = postpaid.getString("checkPostPaidTransactionEndpoint")
  }

  object prepaid{
    private val prepaid = vendorPointConfig.getConfig("prepaid")
    val responseUri = prepaid.getString("responseUri")
    val verifyEndpoint = prepaid.getString("verifyEndpoint")
    val vendEndpoint = prepaid.getString("vendEndpoint")
    val checkTransactionEndPoint = prepaid.getString("checkTransactionEndPoint")
  }
}