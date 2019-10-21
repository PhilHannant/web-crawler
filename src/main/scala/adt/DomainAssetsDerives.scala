package adt

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import adt.UrlAssetsDerives._

object DomainAssetsDerives {

  implicit val domainsDecoder: Decoder[DomainAssets] = deriveDecoder
  implicit val domainsEncoder: Encoder[DomainAssets] = deriveEncoder

}
