
export default class UserProfile {

    static currentUser?:UserProfile;

    lastName?:string;
    gender?:string;
    birthYear?:number;

    constructor(
        public firstName:string,
        public email:string,
        public initials:string,
        public sampleBaseId:string) {
    }

    static fullName(p:UserProfile) {
        let result = p.firstName;
        if (p.lastName) {
            result += " " + p.lastName;
        }
        return result;
    }

    static fromJson(input:any) {
        let result = new UserProfile(input.firstName, input.email, input.initials, input.sampleBaseId);
        result.lastName = input.lastName;
        result.gender = input.gender;
        result.birthYear = input.birthYear;
        return result;
    }

    static getCurrent():UserProfile {
        return this.currentUser!;
    }

    static setCurrent(newProfile:UserProfile) {
        this.currentUser = newProfile;
    }

    static unsetCurrent() {
        delete this.currentUser;
    }

}
