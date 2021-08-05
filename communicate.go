package main


import(
	"fmt"
	"net"
)

type User struct {
	id   string
	name string
	msg  chan string
}

//定义一个key-value变量(map结构),并make出来空间
var allUsers = make(map[string]User)

//全局管道，进行广播
var message = make(chan string, 10)

//读取用户信息
func Read(con net.Conn) {
	data := make([]byte, 1000)
	for{
		n, err := con.Read(data)
		if err != nil{
			fmt.Println(err)
			break
		}
		fmt.Println(string(data[:n]))
	}
}

//广播消息
func broadcast1() {
	fmt.Println("服务器端提示：广播启动成功...")
	defer fmt.Println("服务器端提示：广播异常退出...")

	for {
		//拿出广播通道message数据
		info := <-message
		fmt.Println("广播消息进程：info",info)
		//遍历全部用户，将广播推送到每一个用户msg通道中
		for _, user := range allUsers {
			user.msg <- info
		}
	}
}

//给用户发送消息
func writebackToClient1(user *User ,conn net.Conn){
	fmt.Println("服务器端提示：writebackToClient启动...")
	for {
		//取出user.msg数据，以tcp连接形式把数据发送到每一个user
		data := <-user.msg //必须加上 <-,把msg通道数据拿出来
		fmt.Println("写回客户端数据：",data)
		conn.Write([]byte(data))
	}
}
//退出
func quit1(user *User ,conn net.Conn){
	message <- fmt.Sprintf("username:%s主动退出", user.name)
	delete(allUsers, user.id)
	conn.Close()
}

//连接完成，处理业务
func handle_fun(con net.Conn){
	//返回相应数据给客户端
	clientAddr := con.RemoteAddr().String()
	//初始化newUser
	newuser := User{
		id:   clientAddr,
		name: clientAddr,
		msg:  make(chan string),
	}
	//加入用户集合
	allUsers[newuser.id] = newuser
	//返回数据给客户端,以多进程的形式返回用户msg数据

	go writebackToClient1(&newuser,con)


	//上线广播
	//格式化拼接用户上线数据
	msginfo := fmt.Sprintf("id:%s,name:%s 上线了...\n", newuser.id, newuser.name)
	message <- msginfo


	//获取客户端输入数据，所出相应判断
	data := make([]byte, 1000)
	for{
		n, err := con.Read(data)
		message <- string(data[0:n])
		fmt.Println("n",n)
		//退出
		if n == 0 {
			quit1(&newuser ,con)
			//下线广播
			msginfo := fmt.Sprintf("id:%s,name:%s 下线了...\n", newuser.id, newuser.name)
			message <- msginfo
		}
		if err != nil{
			fmt.Println(err)
			break
		}
		fmt.Println(string(data[0:n]))
	}

	//退出账户，删除该user
	//下线广播

}

func main(){
	//开始监听
	listen, err := net.Listen("tcp", "192.168.79.1:8080")
	if err != nil{
		fmt.Println(err)
		return
	}
	//监听成功
	for{
		//开始接受客户端请求
		con, err := listen.Accept()
		if err != nil{
			fmt.Println(err)
			continue
		}
		//有新用户加入群聊，开始读取用户数据请求，并广播
		go broadcast1()
		go handle_fun(con)
	}
}
