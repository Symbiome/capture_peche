
export default class Constants {

    static baseApiUrl():string {
        let result = process.env.VUE_APP_API_URL;
        if (!result) {
            result = location.protocol + "//" + location.hostname;
            if (process.env.VUE_APP_API_DEFAULT_PORT) {
                result += ":" + process.env.VUE_APP_API_DEFAULT_PORT
            }
            result += "/api";
        }
        return result;
    }

    static apiUrl(path:string):string {
        let result = Constants.baseApiUrl() + path;
        return result;
    }

}
