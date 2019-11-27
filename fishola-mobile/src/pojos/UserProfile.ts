
export default class UserProfile {

    lastName?:string;
    gender?:string;
    birthYear?:number;

    constructor(
        public firstName:string,
        public email:string,
        public initials:string) {
    }

    fullName() {
        return this.firstName + " " + this.lastName;
    }

    static fromJson(input:any) {
        let result = new UserProfile(input.firstName, input.email, input.initials);
        result.lastName = input.lastName;
        result.gender = input.gender;
        result.birthYear = input.birthYear;
        console.log("Profile parsed: ", result);
        return result;
    }

}
