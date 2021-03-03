const toolbarOptions = {
    container: [
        ["bold", "italic", "underline", "strike"], // toggled buttons
        ["blockquote", "code-block"],
        [{header: 1}, {header: 2}], // custom button values
        [{list: "ordered"}, {list: "bullet"}],
        [{script: "sub"}, {script: "super"}], // superscript/subscript
        [{indent: "-1"}, {indent: "+1"}], // outdent/indent
        [{direction: "rtl"}], // text direction
        [{size: ["small", false, "large", "huge"]}], // custom dropdown
        [{header: [1, 2, 3, 4, 5, 6, false]}],
        [{color: []}, {background: []}], // dropdown with defaults from theme
        [{font: []}],
        [{align: []}],
        ["clean"]
    ]
}

const app = new Vue({
    el: '#app',
    delimiters: ['[[', ']]'],
    data() {
        return {
            isLoading: false,
            examFile: null,
            notificationTitle: '',
            notificationContent: '',
            editorOption: {
                modules: {
                    toolbar: toolbarOptions
                }
            },
            messageLength: 0,
            maxLength: 2000,
            isDebug: true
        }
    },
    watch: {
        notificationContent: function (val) {
            this.messageLength = val.length
        }
    },
    created() {
        const self = this
        // Only initialize state listener on login page
        if (window.location.pathname === '/app/login') {
            this.isLoading = true
            firebase.auth().onAuthStateChanged(async (firebaseUser) => {
                console.log('user state changed')
                this.isLoading = false
                // unsubscribe()
                if (firebaseUser) {
                    try {
                        const idToken = await firebaseUser.getIdToken()
                        await self.sendTokenToBackend(idToken)
                    } catch (error) {
                        console.error(error)
                    }
                } else {
                    console.log('User has signed out')
                }
            });
        }
    },
    methods: {
        async login() {
            this.isLoading = true
            await firebase.auth().setPersistence(firebase.auth.Auth.Persistence.SESSION)
            const provider = new firebase.auth.GoogleAuthProvider()
            try {
                const result = await firebase.auth().signInWithPopup(provider)
            } catch (error) {
                console.error(error)
            }
        },
        async sendTokenToBackend(idToken) {
            this.isLoading = true
            const params = new URLSearchParams()
            params.append('id_token', idToken)
            try {
                const res = await axios.post(window.location.pathname, params, {
                    headers: {
                        'X-My-Portal': ''
                    }
                })
                if (res.status === 200 || res.status === 302) {
                    window.location = res.request.responseURL
                }
            } catch (e) {
                console.error(e)
            } finally {
                this.isLoading = false
            }
        },
        logout() {
            firebase.auth().signOut().then(function () {
                window.location = '/app/logout'
            }).catch(function (error) {
                console.error(error)
            });
        },
        async clearExamSchedule() {
            const ret = confirm("Are you sure you want to clear the exam schedule?")
            if (ret === true) {
                this.isLoading = true
                try {
                    await axios.post(`${window.location.pathname}/delete`, {}, {
                        headers: {
                            'X-My-Portal': ''
                        }
                    })
                    this.$bvToast.toast(`Schedule cleared successfully`, {
                        autoHideDelay: 5000,
                        appendToast: false
                    })
                    location.reload()
                } catch (e) {
                    console.error(e)
                    this.$bvToast.toast(`Failed to clear schedule`, {
                        autoHideDelay: 5000,
                        appendToast: false
                    })
                } finally {
                    this.isLoading = false
                }

            }
        },
        async uploadSchedule() {
            this.isLoading = true
            try {
                const formData = new FormData()
                formData.append("file", this.examFile)
                await axios.post(`${window.location.pathname}/upload`, formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                        'X-My-Portal': ''
                    }
                })
                this.$bvToast.toast(`Exam schedule uploaded successfully`, {
                    autoHideDelay: 5000,
                    appendToast: false
                })
                location.reload()
            } catch (e) {
                console.error(e)
                this.$bvToast.toast(`Failed to upload exam schedule`, {
                    autoHideDelay: 5000,
                    appendToast: false
                })
            } finally {
                this.isLoading = false
            }
        },
        onEditorReady(quill) {
            console.log("editor ready!");
        },
        async sendNotification() {
            if (!this.notificationTitle || !this.notificationContent) {
                this.$bvToast.toast(`Both fields must be filled`, {
                    autoHideDelay: 5000,
                    appendToast: false,
                    title: 'Missing fields'
                })
                return
            }

            const ret = confirm("Are you sure you want to send this notification?")
            if (ret === true) {
                this.isLoading = true
                const topic = this.isDebug ? 'debug' : 'messages'
                const body = htmlToPlainText(this.notificationContent)
                const params = new URLSearchParams()
                params.append('topic', topic)
                params.append('title', this.notificationTitle)
                params.append('body', body)
                try {
                    const res = await axios.post(window.location.pathname, params, {
                        headers: {
                            'X-My-Portal': ''
                        }
                    })
                    this.$bvToast.toast(`Notification sent successfully!`, {
                        autoHideDelay: 5000,
                        appendToast: false,
                        title: 'Success'
                    })
                } catch (e) {
                    console.error(e)
                    this.$bvToast.toast(`Failed to send notification`, {
                        autoHideDelay: 5000,
                        appendToast: false,
                        title: 'Failed'
                    })
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
})