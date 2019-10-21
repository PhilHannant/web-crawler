package adt

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object UrlAssetsDerives {

  implicit val domainDecoder: Decoder[UrlAssets] = deriveDecoder
  implicit val domainEncoder: Encoder[UrlAssets] = deriveEncoder

}
