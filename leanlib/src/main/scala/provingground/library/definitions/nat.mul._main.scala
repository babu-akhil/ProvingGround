package provingground.library
import provingground._
import HoTT._
import induction._
import implicits._
import shapeless._
import Fold._
object nat$mul$_main {
  lazy val value = lmbda("'aj_641191407" :: "nat" :: Type)(lmbda("'ak_957604511" :: "nat" :: Type)(({
    val rxyz = pprodInd.value(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'ak_957604511" :: "nat" :: Type)).rec(FuncTyp("nat" :: Type, "nat" :: Type))
    rxyz
  })(lmbda("'s_1338792767" :: FuncTyp("nat" :: Type, "nat" :: Type))(lmbda("_" :: ({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'ak_957604511" :: "nat" :: Type))("'s_1338792767" :: FuncTyp("nat" :: Type, "nat" :: Type))))(({
    val rxyz = natInd.value.induc(lmbda("'v_877202618" :: "nat" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_877202618" :: "nat" :: Type))))
    rxyz
  })(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(FuncTyp("nat" :: Type, "nat" :: Type))("punit" :: Type)(lmbda("'an_1191940455" :: "nat" :: Type)("nat.zero" :: "nat" :: Type))("punit.star" :: "punit" :: Type))(lambda("'v_1605754716" :: "nat" :: Type)(lmbda("'w_406339356" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)))(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(FuncTyp("nat" :: Type, "nat" :: Type))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)))("punit" :: Type))(lmbda("'an_1191940455" :: "nat" :: Type)(({
    val rxyz = pprodInd.value(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'an_1191940455" :: "nat" :: Type)).rec(FuncTyp("nat" :: Type, "nat" :: Type))
    rxyz
  })(lmbda("'s_1037319927" :: FuncTyp("nat" :: Type, "nat" :: Type))(lmbda("_" :: ({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'an_1191940455" :: "nat" :: Type))("'s_1037319927" :: FuncTyp("nat" :: Type, "nat" :: Type))))(({
    val rxyz = natInd.value.induc(lmbda("'v_877202618" :: "nat" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_877202618" :: "nat" :: Type))))
    rxyz
  })(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(FuncTyp("nat" :: Type, "nat" :: Type))("punit" :: Type)(lmbda("'ad_797428180" :: "nat" :: Type)("'ad_797428180" :: "nat" :: Type))("punit.star" :: "punit" :: Type))(lambda("'v_129059827" :: "nat" :: Type)(lmbda("'w_1265018266" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)))(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(FuncTyp("nat" :: Type, "nat" :: Type))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)))("punit" :: Type))(lmbda("'ad_797428180" :: "nat" :: Type)(("nat.succ" :: FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = pprodInd.value(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("_" :: "nat" :: Type)(lmbda("'ak_558478017" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'ak_558478017" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)).rec(FuncTyp("nat" :: Type, "nat" :: Type))
    rxyz
  })(lmbda("'s_1559554715" :: FuncTyp("nat" :: Type, "nat" :: Type))(lmbda("_" :: ({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("_" :: "nat" :: Type)(lmbda("'ak_558478017" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'ak_558478017" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type))("'s_1559554715" :: FuncTyp("nat" :: Type, "nat" :: Type))))("'w_1265018266" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)))("'ad_797428180" :: "nat" :: Type))))(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)))("punit" :: Type)("'w_1265018266" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_129059827" :: "nat" :: Type)))("punit.star" :: "punit" :: Type)))))("'an_1191940455" :: "nat" :: Type))(({
    val rxyz = pprodInd.value(FuncTyp("nat" :: Type, "nat" :: Type))(({
      val rxyz = natInd.value.rec(Type)
      rxyz
    })("punit" :: Type)(lmbda("_" :: "nat" :: Type)(lmbda("'au_1747314004" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'au_1747314004" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)).rec(FuncTyp("nat" :: Type, "nat" :: Type))
    rxyz
  })(lmbda("'s_1516530190" :: FuncTyp("nat" :: Type, "nat" :: Type))(lmbda("_" :: ({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("_" :: "nat" :: Type)(lmbda("'au_1747314004" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'au_1747314004" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type))("'s_1516530190" :: FuncTyp("nat" :: Type, "nat" :: Type))))("'w_406339356" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)))("'an_1191940455" :: "nat" :: Type))))(("pprod.mk" :: piDefn("'f_1394520732" :: Type)(piDefn("'g_1655163109" :: Type)(FuncTyp("'f_1394520732" :: Type, FuncTyp("'g_1655163109" :: Type, ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))("'f_1394520732" :: Type)("'g_1655163109" :: Type))))))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)))("punit" :: Type)("'w_406339356" :: ("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))(({
    val rxyz = natInd.value.rec(Type)
    rxyz
  })("punit" :: Type)(lmbda("'h_1573352955" :: "nat" :: Type)(lmbda("'i_830893068" :: Type)(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(("pprod" :: FuncTyp(Type, FuncTyp(Type, Type)))(FuncTyp("nat" :: Type, "nat" :: Type))("'i_830893068" :: Type))("punit" :: Type))))("'v_1605754716" :: "nat" :: Type)))("punit.star" :: "punit" :: Type)))))("'ak_957604511" :: "nat" :: Type))("'aj_641191407" :: "nat" :: Type)))
}
