import io.github.vrchatapi.ApiException
import io.github.vrchatapi.Configuration
import io.github.vrchatapi.api.AuthenticationApi
import io.github.vrchatapi.api.InviteApi
import io.github.vrchatapi.api.SystemApi
import io.github.vrchatapi.api.UsersApi
import io.github.vrchatapi.auth.ApiKeyAuth
import io.github.vrchatapi.auth.HttpBasicAuth
import io.github.vrchatapi.model.CurrentUser
import io.github.vrchatapi.model.Instance
import io.github.vrchatapi.model.InviteRequest
import io.github.vrchatapi.model.TwoFactorAuthCode
import kotlin.system.exitProcess


fun main() {
    val defaultClient = Configuration.getDefaultApiClient()
    val authApi = AuthenticationApi(defaultClient)

    val authHeader = defaultClient.getAuthentication("authHeader") as HttpBasicAuth

    authHeader.username = "un"
    authHeader.password = "pw"

    var userJSON: String? = null

    try {
        val user = authApi.currentUser
        println(user.displayName)
    }
    catch (e: IllegalArgumentException) { // require2FA머시기 필드 illegalarg에러
        val totpCode = readln()
        println(authApi.verify2FAWithHttpInfo(TwoFactorAuthCode().code(totpCode)).statusCode)
        try { // Exception in thread "main" java.lang.IllegalArgumentException: The field acceptedPrivacyVersion in the JSON string is not defined in the CurrentUser properties.  JSON: {"id":"usr_//이하생략
            val user = authApi.currentUser
            println("Logged in as ${user.displayName}")
        }
        catch (e: IllegalArgumentException) { // 에러가 나지만 일단 json자체는 제대로 출력 되는 것 같아서 만들어둠. acceptedPrivacyVersion이 뭔지 알아내는대로 아래 코드는 제거하시오.
            val regex = Regex("(\\{.*})")
            userJSON = regex.find(e.localizedMessage as CharSequence)!!.value
            println("Logged in as ${getValueWithStringifiedJSON(userJSON, "displayName")} (Fallback Method)")
        }
    }

    println(userJSON!!)

    val usersApi = UsersApi(defaultClient)
    val inviteApi = InviteApi(defaultClient)
    val world = getValueWithStringifiedJSON(userJSON, "world")
    val instance = getValueWithStringifiedJSON(userJSON, "instance")
    val instanceId = "$world:$instance"

    // The field `receiverUserId` in the JSON string is not defined in the `SentNotification` properties. 에러. sdk 개발 손 놓은건가??
    // 에러창에 출력된 JSON에는 receiverUserId 프로퍼티 존재함. 위의 accepted머시기도 동일
    // sdk 자체에 무슨 문제가 있는 듯 하다. try/catch를 떡칠하던가 C#같은걸로 갈아타던가 내가 수동으로 http 통신 하던가...
    println(inviteApi.inviteUser("usr_d7ed0bd9-b5e7-4aea-a7ce-1e2bac000d81", InviteRequest().instanceId(instanceId)).toJson())
}

fun getValueWithStringifiedJSON(json: String, key: String): String? {
    val regex = Regex("\"$key\":\"([^\"]*)\"")
    val matchResult = regex.find(json as CharSequence)

    matchResult?.value?.let {
        return it.split('"')[3]
    }

    return null
}