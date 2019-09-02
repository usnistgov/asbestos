export default{
    data() {
        return {
            monthNames: ['Jan', 'Feb', 'Mar', 'April', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'],
        }
    },
    methods: {
        eventAsDate(name) {
            const parts = name.split('_')
            // const year = parts[0]
            const month = parts[1]
            const day = parts[2]
            const hour = parts[3]
            const minute = parts[4]
            const second = parts[5]
            const milli = parts[6]
            const monthName = this.monthNames[+month]
            //return name
            return `${day} ${monthName} ${hour}:${minute}:${second}:${milli}`
        },
    },
}
