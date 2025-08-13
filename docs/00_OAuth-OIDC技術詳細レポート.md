

# **「Googleでログイン」実装のためのOAuth/OIDC技術詳細レポート**

## **第1部 現代的Web認証の基礎原理**

Webアプリケーションにおけるユーザー認証・認可の仕組みを正しく構築するためには、その根幹をなす技術的原則を深く理解することが不可欠です。特に、「Googleでログイン」のようなソーシャルログイン機能を実装する際には、OAuth 2.0とOpenID Connect (OIDC) という二つの標準規格が中心的な役割を果たします。しかし、これら二つの技術の目的と役割を混同することは、深刻なセキュリティ脆弱性を生み出す原因となります。本章では、まずOAuth 2.0を「認可」のフレームワークとして、次にOIDCをその上に構築された「認証」のレイヤーとして、それぞれの概念を厳密に切り分け、その理論的基盤を解説します。

### **1.1 OAuth 2.0の解体：認可フレームワーク**

OAuth 2.0は、しばしば認証プロトコルであると誤解されますが、その本質は**委任認可 (Delegated Authorization)** のためのフレームワークです 1。これは、ユーザー（リソースオーナー）が自身の認証情報（例：Googleのパスワード）を第三者のアプリケーション（クライアント）に直接渡すことなく、そのアプリケーションに対して、特定のリソースへの限定的なアクセス権を安全に付与するための仕組みを定めています 1。

#### **4つの主要な役割**

OAuth 2.0の仕様は、エコシステムを構成する4つの主要な役割を定義しています 1。

* **リソースオーナー (Resource Owner)**: 保護されたリソース（データ）を所有し、それへのアクセスを許可する権限を持つエンティティです。一般的にはエンドユーザーを指します。「Googleでログイン」の文脈では、Googleアカウントを持つ個人がこれに該当します。  
* **クライアント (Client)**: リソースオーナーに代わって、保護されたリソースへのアクセスを要求するアプリケーションです。貴社が開発するWebサイトやモバイルアプリがこれにあたります。  
* **認可サーバー (Authorization Server)**: リソースオーナーを認証し、その同意を得た上で、クライアントに対してアクセストークンを発行するサーバーです。Googleの認証システムがこの役割を担います。認可サーバーは、ユーザーとの対話的な認証と同意を処理する「認可エンドポイント」と、アプリケーションとの機械的な通信でトークンを発行する「トークンエンドポイント」という2つの主要なAPIエンドポイントを公開します 2。  
* **リソースサーバー (Resource Server)**: 保護されたリソースをホストするサーバーです。クライアントから提示されたアクセストークンを検証し、リクエストされたリソースへのアクセスを許可または拒否します。Googleのプロフィール情報APIやカレンダーAPIなどがこれに該当します。

#### **主要な概念と成果物**

OAuth 2.0フローの中では、いくつかの重要な概念と、それによって生成される「成果物（Artifacts）」が存在します。

* **アクセストークン (Access Token)**: クライアントがリソースサーバーに対して認証済みのリクエストを行うために使用する、一時的な資格情報です 1。これは、特定のリソースオーナーに代わって、特定の権限（スコープ）の範囲内で操作を行うための「鍵」のようなものです。セキュリティ上の理由から、アクセストークンには通常、短い有効期限が設定されています 2。そのフォーマットはOAuth 2.0の仕様では規定されておらず、JSON Web Token (JWT) 形式が用いられることもあれば、単なる不透明な文字列の場合もあります 2。  
* **リフレッシュトークン (Refresh Token)**: アクセストークンの有効期限が切れた際に、ユーザーに再度の認証を求めることなく、新しいアクセストークンを取得するために使用される、より長寿命の資格情報です 3。これにより、ユーザー体験を損なうことなく、セッションの継続性を保つことができます。  
* **スコープ (Scope)**: クライアントが要求するアクセス権の範囲を定義する仕組みです 2。例えば、「ユーザーの基本プロフィールの読み取り」や「カレンダーへのイベント追加」といった具体的な権限を文字列で指定します。これにより、アプリケーションは必要最小限の権限のみを要求することができ、  
  **最小権限の原則 (Principle of Least Privilege)** を実現します 6。ユーザーは同意画面で、どのスコープのアクセスを許可するかを確認できます。

これらの要素が組み合わさることで、OAuth 2.0はユーザーのパスワードを共有することなく、安全にサービス間のデータ連携を可能にする堅牢な基盤を提供します。しかし、ここで最も重要な点は、OAuth 2.0が本質的に「認可」に特化しているという事実です。

純粋なOAuth 2.0のアクセストークンは、その保持者が「リソースXにアクセスする権限を持つ」こと（**認可**）を証明しますが、その保持者が「誰であり、いつ認証されたのか」（**認証**）を直接的に証明するものではありません 8。アクセストークンは単なる権限の証明であり、それ自体に検証可能なユーザーの身元情報が含まれているとは限りません。もし攻撃者がアクセストークンを窃取した場合、リソースサーバーはトークンが有効である限りリクエストを受け入れてしまい、なりすましを防ぐことができません。この認証機能の欠如こそが、OAuth 2.0だけを用いてログインシステムを構築することが、深刻なセキュリティ上のアンチパターンとされる理由です。この根本的なギャップを埋めるために、OpenID Connectが策定されました。

### **1.2 OpenID Connect (OIDC) の導入：認証レイヤー**

「Googleでログイン」機能の核心は、OAuth 2.0を基盤としつつ、その上に認証機能を追加した**OpenID Connect (OIDC)** というプロトコルにあります 1。OIDCは、OAuth 2.0の認可フローを利用して、標準化された安全な

**認証**メカニズムを提供する、薄いアイデンティティレイヤーです 12。

#### **OIDCの決定的な追加要素：IDトークン**

OIDCがOAuth 2.0と根本的に異なる点は、**IDトークン (ID Token)** という新たな成果物を導入したことです 8。IDトークンは、

**JSON Web Token (JWT)** という標準形式（RFC 7519）でエンコードされた、ユーザーの認証に関する情報を含むオブジェクトです。これは、認可サーバー（OIDCではOpenIDプロバイダーと呼ばれる）によって発行され、クライアント（OIDCではリライングパーティと呼ばれる）に渡されます。

IDトークンには、ユーザー認証イベントに関する検証可能な情報、すなわち「クレーム (Claims)」が含まれています。代表的なクレームは以下の通りです 15。

* iss (Issuer): トークンの発行者。Googleの場合は https://accounts.google.com となります。  
* sub (Subject): エンドユーザーを一意に識別するための、安定的で不変の識別子。**これは、自社データベースでユーザーを識別するための最も重要なキーとなります。**  
* aud (Audience): トークンの対象者。貴社アプリケーションのクライアントIDが入ります。これにより、トークンが意図したアプリケーション以外で利用されることを防ぎます。  
* exp (Expiration Time): トークンの有効期限を示すタイムスタンプ。  
* iat (Issued At): トークンが発行された時刻を示すタイムスタンプ。  
* nonce (Number used once): リプレイ攻撃（一度使われた認証情報を再利用する攻撃）を緩和するための、リクエストごとに生成される一意な値。

IDトークンは、OpenIDプロバイダーの秘密鍵によって電子署名されています。リライングパーティ（貴社アプリケーション）は、プロバイダーが公開する公開鍵を用いてこの署名を検証することで、トークンが本物であり、その内容（ユーザーIDやメールアドレスなど）が改ざんされていないことを暗号学的に確認できます 13。

#### **用語の変化**

OIDCの導入に伴い、OAuth 2.0の役割の名称も以下のように変化します。

* **OpenIDプロバイダー (OpenID Provider, OP)**: 認証を提供し、IDトークンを発行するエンティティ（例：Google）。OAuth 2.0の認可サーバーに相当します 8。  
* **リライングパーティ (Relying Party, RP)**: OPに認証を要求するアプリケーション（例：貴社Webサイト）。OAuth 2.0のクライアントに相当します 8。

このように、OIDCはOAuth 2.0の抽象的な「アクセス権」という概念を、具体的で検証可能な「アイデンティティ情報」へと昇華させます。それは、「誰が、いつログインしたのか」という問いに対して、OAuth 2.0単体では提供できなかった暗号学的に安全な答えを提供します。したがって、現代的なソーシャルログイン機能は、OAuth 2.0の認可フローとOIDCのIDトークンを組み合わせることで、初めて安全かつ完全に実現されるのです。

## **第2部 「Googleでログイン」認証フローの実践**

理論的基盤を理解した上で、次はこの知識を具体的な実装プロセスに落とし込みます。本章では、「Googleでログイン」機能を実現するために最も推奨される「認可コードグラントフロー」を詳細に分析し、Google Cloud Platform (GCP) での具体的な設定手順、そしてユーザー情報を取得するためのスコープの扱い方までを網羅した、実践的なガイドを提供します。

### **2.1 認可コードグラントフロー：ステップ・バイ・ステップ分析**

認可コードグラントフロー (Authorization Code Grant Flow) は、サーバーサイドのコンポーネントを持つWebアプリケーションにおいて、最も一般的かつ安全なOAuth 2.0の認可方式です 6。このフローの最大の特徴は、アクセストークンやリフレッシュトークンといった機密性の高い資格情報が、ユーザーのブラウザ（フロントエンド）に直接渡されることなく、アプリケーションのサーバー（バックエンド）と認可サーバーとの間で安全に交換される点にあります 3。これにより、トークン漏洩のリスクを大幅に低減できます。

#### **フローの詳細な分解**

以下に、ユーザーが「Googleでログイン」ボタンをクリックしてから、アプリケーション内でセッションが確立されるまでの一連の流れを、シーケンスに沿って詳細に解説します 4。

1. **クライアント登録（事前準備）**: フローを開始する前に、貴社のアプリケーションをGoogle Cloud Platformに登録し、client\_id（クライアントID）とclient\_secret（クライアントシークレット）を取得しておく必要があります。これは、アプリケーションをGoogleに対して一意に識別するためのものです 17。  
2. **ユーザーによるログイン開始**: ユーザーが貴社サイト上の「Googleでログイン」ボタンをクリックします。  
3. **認可リクエスト**: アプリケーションのバックエンドは、必要なパラメータ（後述の表1を参照）を含むURLを構築し、ユーザーのブラウザをGoogleの認可エンドポイント（例: https://accounts.google.com/o/oauth2/v2/auth）へリダイレクトさせます 15。このリクエストは、ユーザーに代わって権限を要求する意思表示です。  
4. **ユーザー認証と同意**: Googleは、ユーザーがGoogleアカウントにログインしているか確認し、必要であればログインを要求します。その後、貴社アプリケーションが要求している権限（スコープ）の一覧を記載した「同意画面」を表示し、ユーザーにアクセス許可を求めます 1。  
5. **認可コードの付与**: ユーザーが同意すると、Googleはブラウザを、クライアント登録時に指定した貴社アプリケーションのredirect\_uri（リダイレクトURI）へ再度リダイレクトさせます。このとき、URLのクエリパラメータとして、短命かつ一度しか使えない**認可コード (Authorization Code)** が付与されます 3。  
6. **トークン交換**: 貴社アプリケーションのバックエンドは、リダイレクトURIでこの認可コードを受け取ります。次に、バックエンドからGoogleのトークンエンドポイント（例: https://oauth2.googleapis.com/token）に対して、安全なサーバー間通信（HTTPS POSTリクエスト）を行います。このリクエストには、受け取った認可コード、自らのclient\_id、そして厳重に保管しているclient\_secretが含まれます 1。  
7. **トークン応答**: Googleはリクエストを検証し、正当であれば、access\_token、refresh\_token（要求した場合）、そして認証の証明であるid\_tokenを含むJSONオブジェクトをレスポンスとして返します 15。  
8. **IDトークン検証とユーザーセッション確立**: バックエンドは受け取ったid\_tokenの署名とクレームを検証します。検証に成功すれば、トークンからユーザー情報（特にsubクレーム）を抽出し、そのユーザーに対応するセッションをアプリケーション内で作成してログイン状態とします 13。

#### **Table 1: 認可リクエストの主要パラメータ**

認可リクエストURLを正しく構築することは、フローを開始するための最初の重要なステップです。以下に主要なパラメータをまとめます。

| パラメータ | 必須/推奨 | 説明 | 設定例 |
| :---- | :---- | :---- | :---- |
| client\_id | 必須 | GCPで発行されたアプリケーションのクライアントID 17。 | 12345.apps.googleusercontent.com |
| redirect\_uri | 必須 | Googleが認可コードを付与してリダイレクトさせる先のURL。GCPで事前に登録したものと完全に一致する必要があります 15。 | https://myapp.com/callback/google |
| response\_type | 必須 | 認可コードフローでは、必ずcodeを指定します 17。 | code |
| scope | 必須 | 要求する権限の範囲をスペース区切りで指定します。OIDC認証にはopenidが必須です 15。 | openid email profile |
| state | 推奨/必須 | CSRF攻撃を防ぐための、推測困難なランダムな文字列。サーバー側で生成・検証します 17。 | aF0W8qV2z |
| nonce | 推奨/必須 | リプレイ攻撃を防ぐための、リクエストごとに生成される一意な値。IDトークン内に返却され、検証されます 15。 | n-0S6\_WzA2Mj |
| code\_challenge | 推奨/必須 | PKCEで使用。code\_verifierから生成されたハッシュ化された文字列 20。 | E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM |
| code\_challenge\_method | 推奨/必須 | PKCEで使用。code\_challengeの生成方法。通常はS256を使用します 20。 | S256 |

#### **Table 2: トークン応答の構造**

トークン交換が成功すると、バックエンドは以下の構造を持つJSONオブジェクトを受け取ります。

| フィールド | 型 | 説明 | 主な用途 |
| :---- | :---- | :---- | :---- |
| access\_token | String | リソースサーバー（Google API）へのアクセスに使用するトークン。 | Google API（例: UserInfo API）へのリクエストのAuthorizationヘッダーに含める。 |
| id\_token | String | ユーザーの認証情報を含むJWT。OIDCの核心部分です。 | 署名を検証し、ペイロードからユーザーID (sub) などを取得して認証を完了させる。 |
| refresh\_token | String | access\_tokenの有効期限が切れた際に、新しいaccess\_tokenを取得するために使用するトークン。 | サーバーサイドで安全に保管し、アクセストークンの更新時に使用する。 |
| expires\_in | Integer | access\_tokenの有効期間（秒）。 | アクセストークンの有効期限を管理するために使用する。 |
| scope | String | 実際に許可された権限のスコープ。リクエストしたものと異なる場合があります。 | アプリケーションが利用可能な権限を確認するために使用する。 |
| token\_type | String | トークンの種類。通常はBearerです 20。 | APIリクエスト時のAuthorizationヘッダーの形式（Bearer {access\_token}）を指定する。 |

### **2.2 実装ガイド：Google Cloudプロジェクトの設定**

「Googleでログイン」を実装するための最初のステップは、Google Cloud Platform (GCP) で必要な設定を行うことです。このプロセスは、アプリケーションをGoogleの認証システムに登録し、通信に必要な認証情報を取得するために不可欠です。

* ステップ 1: Google Cloud Platform (GCP) プロジェクトの作成  
  すべてのGoogle Cloudリソースは「プロジェクト」という単位で管理されます。まずはGoogle API Consoleにアクセスし、新しいプロジェクトを作成します。プロジェクト名は任意ですが、管理しやすい名前をつけることが推奨されます 21。  
* ステップ 2: OAuth同意画面の設定  
  次に、ユーザーがアクセス許可を求められる際に表示される「OAuth同意画面」を設定します 21。これはユーザーに対するアプリケーションの「顔」となるため、正確な情報を提供することが重要です。  
  1. GCPコンソールのナビゲーションメニューから「APIとサービス」\>「OAuth同意画面」を選択します。  
  2. ユーザータイプとして「外部」を選択します（Google Workspace組織外の一般ユーザーからのアクセスを許可する場合）。  
  3. アプリ名、ユーザーサポートメール、デベロッパーの連絡先情報などを入力します。  
  4. 「スコープ」セクションで、アプリケーションが必要とする権限（例: openid, email, profile）を追加します。  
  5. 「承認済みドメイン」に、アプリケーションがホストされているドメインを登録します。これはセキュリティ上の重要な設定です 22。  
* ステップ 3: OAuth 2.0 クライアントIDの作成  
  同意画面の設定が完了したら、アプリケーション自体を識別するための認証情報を作成します。  
  1. 「APIとサービス」\>「認証情報」ページに移動します 26。  
  2. 「+ 認証情報を作成」をクリックし、「OAuthクライアントID」を選択します。  
  3. アプリケーションの種類として「ウェブアプリケーション」を選択します 21。  
  4. **承認済みのリダイレクトURI**のセクションに、アプリケーションのコールバックURL（認可コードを受け取るエンドポイントのURL）を正確に入力します。これは極めて重要なセキュリティ設定であり、Googleはここに登録されたURI以外には認可コードを送信しません。これにより、認可コードが不正なサイトに送信されるのを防ぎます 15。  
* ステップ 4: クライアントIDとクライアントシークレットの取得  
  作成が完了すると、client\_idとclient\_secretが発行されます 15。  
  * **クライアントID (client\_id)**: 公開情報であり、認可リクエストURLに含めて使用されます。  
  * **クライアントシークレット (client\_secret)**: 機密情報であり、パスワードのように厳重に管理する必要があります。**この値はサーバーサイドでのみ使用し、決してクライアントサイドのコード（JavaScriptなど）に埋め込んだり、ネットワーク経由で送信したりしてはいけません。**

### **2.3 ユーザー情報の取得：スコープとデータアクセス**

ユーザー認証が完了した後、アプリケーションはユーザーの情報を利用して、パーソナライズされた体験を提供します。どの情報にアクセスできるかは、認可リクエスト時に指定した「スコープ」によって厳密に制御されます。

#### **最小権限の原則**

セキュリティのベストプラクティスとして、アプリケーションが必要とする**最小限のスコープのみを要求する**ことが強く推奨されます 3。例えば、ユーザーの名前とメールアドレスでログイン機能を実装したいだけなのに、Google Driveの全ファイルへのアクセス権限 (

https://www.googleapis.com/auth/drive) を要求するべきではありません。過剰な権限要求はユーザーに不信感を与え、同意をためらわせる原因となります。また、機密性の高いスコープ（センシティブスコープや制限付きスコープ）を要求する場合、Googleによる厳しいアプリケーション審査が必要になることがあります 28。

#### **ログインのための標準OIDCスコープ**

一般的なログイン機能の実装には、以下の3つの標準スコープが用いられます。

* openid: OIDCリクエストであることを示す必須のスコープ。これを指定することで、レスポンスにIDトークンが含まれるようになります 14。  
* profile: ユーザーの基本的なプロフィール情報（氏名、プロフィール写真のURL、性別など）へのアクセスを許可します。  
* email: ユーザーのプライマリメールアドレスと、そのアドレスが認証済みであるかどうかの情報へのアクセスを許可します。

#### **ユーザーデータの取得方法**

ユーザー情報を取得するには、主に2つの方法があります。

1. **IDトークンから取得する（推奨）**: これが最も効率的な方法です。バックエンドでIDトークンの署名を検証した後、そのペイロード（JSONデータ部分）をデコードすれば、要求したスコープに応じたユーザー情報（sub, name, email, pictureなど）が直接得られます。これにより、追加のAPI呼び出しが不要になります 12。  
2. **userinfoエンドポイントを利用する**: 別の方法として、取得したaccess\_tokenをAuthorizationヘッダーに含めて、GoogleのuserinfoエンドポイントにGETリクエストを送信することでも、同様のユーザープロフィール情報を取得できます 15。

#### **Table 3: 一般的なGoogle APIスコープと取得データ**

開発者が目的のデータにアクセスするために必要なスコープを明確にするため、以下に代表的なスコープとその内容をまとめます。

| スコープ文字列 | 取得可能な主なデータ | 機密性レベル | 主な用途 |
| :---- | :---- | :---- | :---- |
| openid | IDトークン（sub, iss, audなど） | 非センシティブ | OIDC認証の基本。必須。 |
| profile | 氏名、プロフィール写真URL、性別など | 非センシティブ | ユーザープロフィールの表示。 |
| email | プライマリメールアドレス、検証ステータス | 非センシティブ | ユーザーのメールアドレス取得、通知。 |
| https://www.googleapis.com/auth/userinfo.profile | profileスコープと同等 | 非センシティブ | userinfoエンドポイントでのプロフィール取得。 |
| https://www.googleapis.com/auth/userinfo.email | emailスコープと同等 | 非センシティブ | userinfoエンドポイントでのメールアドレス取得。 |
| https://www.googleapis.com/auth/calendar.readonly | カレンダーのイベント情報の読み取り | センシティブ | カレンダー連携機能（閲覧のみ）。 |
| https://www.googleapis.com/auth/drive.file | ユーザーがアプリで開いた、または作成したファイルへのアクセス | 非センシティブ | ファイルピッカー連携など、限定的なファイル操作。 |
| https://www.googleapis.com/auth/drive | Google Drive内の全ファイルへのフルアクセス | 制限付き | Drive全体を操作する高度なアプリケーション。 |

この表は、開発者が最小権限の原則を遵守し、不必要な審査を避けるための指針となります 31。

## **第3部 高度なセキュリティと実装のベストプラクティス**

OAuth 2.0とOIDCを用いた認証システムは、正しく実装されれば非常に安全ですが、そのプロセスには多くの落とし穴が存在します。単純な「ハウツー」ガイドに従うだけでは、巧妙な攻撃に対して脆弱なシステムを構築してしまう危険性があります。本章では、単なる実装手順を超え、堅牢な認証基盤を築くために不可欠な、専門家レベルのセキュリティ対策とベストプラクティスを詳述します。

### **3.1 フローの要塞化：必須のセキュリティメカニズム**

認証フローの各ステップには、特定の脅威からシステムを保護するためのセキュリティ制御が組み込まれています。これらを省略することは、意図的にセキュリティホールを作り出すことに等しく、決して許されません。

#### **stateパラメータによるCSRF対策**

* 脅威: クロスサイト・リクエスト・フォージェリ (CSRF)  
  CSRFは、攻撃者が正規ユーザーを騙して、意図しない操作をWebアプリケーションに実行させる攻撃です。OAuthの文脈では、攻撃者は被害者のセッションを利用して、被害者のアカウントを攻撃者自身のGoogleアカウントに紐づけてしまう、といった攻撃が可能になります 35。  
* 対策: stateパラメータの厳格な利用  
  stateパラメータは、このCSRF攻撃を防ぐための必須のセキュリティ制御です 38。その仕組みは以下の通りです。  
  1. **生成と保存**: ユーザーをGoogleの認可エンドポイントにリダイレクトさせる直前に、アプリケーションのサーバーサイドで、セッションごとに一意かつ推測困難なランダム文字列（state値）を生成します。この値は、ユーザーのサーバーセッションに紐付けて保存します（例: RedisやセッションCookie内）17。  
  2. **送信**: 生成したstate値を、Googleへの認可リクエストURLのパラメータに含めて送信します。  
  3. **返却**: Googleは認証・同意プロセスの後、受け取ったstate値を変更せずに、そのままリダイレクトURIのクエリパラメータに含めて返却します。  
  4. **検証**: アプリケーションのサーバーは、リダイレクトで受け取ったstate値が、ステップ1でユーザーセッションに保存した値と完全に一致するかを検証します。一致しない場合、そのリクエストは不正なものとして破棄しなければなりません 18。

この一連の検証により、認可フローを開始したユーザーと、認可コードを持って戻ってきたユーザーが同一人物であることが保証され、CSRF攻撃を効果的に防ぐことができます。

#### **PKCEによる認可コード横取り攻撃対策**

* 脅威: 認可コードの横取り (Authorization Code Interception)  
  この攻撃は、特にクライアントシークレットを安全に保持できない「パブリッククライアント」（例: モバイルアプリ、ブラウザ上で動作するシングルページアプリケーション(SPA)）にとって深刻な脅威です。悪意のあるアプリケーションが何らかの方法で、正規アプリケーションに返されるはずの認可コードを傍受し、その認可コードを使ってアクセストークンを不正に取得してしまう可能性があります 39。  
* 対策: Proof Key for Code Exchange (PKCE)  
  PKCE（「ピクシー」と発音）は、この認可コード横取り攻撃を防ぐために策定された、認可コードフローの拡張仕様です（RFC 7636）41。  
  1. **code\_verifierの生成**: 認可リクエストの前に、クライアントは暗号学的に安全なランダム文字列であるcode\_verifierを生成します。  
  2. **code\_challengeの生成**: code\_verifierをSHA-256でハッシュ化し、Base64URLエンコードした値であるcode\_challengeを生成します。  
  3. **送信**: 認可リクエスト（フローのステップ3）の際に、このcode\_challengeと、その生成方法（S256）を示すcode\_challenge\_methodをパラメータとして送信します。  
  4. **検証**: クライアントが認可コードをアクセストークンと交換する際（フローのステップ6）、リクエストに元の平文のcode\_verifierを含めて送信します。  
  5. **サーバー側の照合**: 認可サーバーは、受け取ったcode\_verifierを、事前に保存しておいたcode\_challenge\_methodに従って変換（ハッシュ化）し、保存しておいたcode\_challengeと一致するかを検証します 17。

この仕組みにより、たとえ認可コードが傍受されても、元のcode\_verifierを知らない攻撃者はアクセストークンを取得できません。これにより、認可リクエストを開始したクライアントと、トークンを要求するクライアントが同一であることが保証されます。元々はパブリッククライアントのために考案されましたが、現在ではすべてのクライアントタイプ（コンフィデンシャルクライアントを含む）でPKCEを使用することが、多層防御の観点からベストプラクティスとされています 16。

### **3.2 安全なトークンとセッションの管理**

認証フローの完了後に得られるトークンは、ユーザーの権限そのものであり、その管理方法はシステムのセキュリティを左右する最も重要な要素の一つです。

#### **トークン保管場所の決定**

* **ブラウザストレージの脆弱性**: アクセストークンやリフレッシュトークンをブラウザのlocalStorageやsessionStorageに保存する手法は、**強く非推奨**です。これらのストレージはJavaScriptから容易にアクセス可能であるため、クロスサイトスクリプティング（XSS）攻撃に対して脆弱です。攻撃者がサイトに悪意のあるスクリプトを注入できた場合、ストレージ内のトークンが窃取され、ユーザーアカウントが乗っ取られる可能性があります 45。  
* SPAのベストプラクティス: Backend for Frontend (BFF) パターン  
  現代のSPAにおける最も安全なアプローチは、BFFパターンを採用することです 47。  
  1. SPA（フロントエンド）は、OAuth/OIDCのトークンを一切扱いません。  
  2. フロントエンド専用のバックエンド（BFF）が、OAuthフローを完了させ、取得したアクセストークンとリフレッシュトークンをサーバーサイドのセッションストア（例: Redis、暗号化されたDB）に安全に保管します。  
  3. BFFは、SPAに対して、従来のWebアプリケーションと同様の、セキュアなHttpOnly属性付きのセッションCookieを発行します。  
  4. SPAが保護されたAPIを呼び出す際は、BFFに対してリクエストを送信します。BFFはセッションCookieを検証し、セッションストアから対応するアクセストークンを取得して、下流のAPIサービスへリクエストを代理送信（プロキシ）します。このアーキテクチャにより、すべてのOAuthトークンがブラウザから完全に隔離されます 48。  
* **従来のWebアプリケーションのベストプラクティス**: サーバーサイドのセッションにトークンを保存します。これはこのアーキテクチャの基本的なモデルであり、安全です 50。  
* **ネイティブモバイルアプリのベストプラクティス**: OSが提供する安全な暗号化ストレージ（iOSのKeychain、AndroidのEncryptedSharedPreferences/Keystore）にトークンを保存します 45。

#### **トークンのライフサイクル管理**

* **アクセストークンの有効期限**: アクセストークンは、漏洩した際の影響を最小限に抑えるため、有効期間を短く設定するべきです（例: 15分～60分）45。  
* **リフレッシュトークンのローテーション**: これは極めて重要なセキュリティ対策です。リフレッシュトークンを使用して新しいアクセストークンを取得するたびに、認可サーバーは**新しいリフレッシュトークンも同時に発行し、使用済みの古いリフレッシュトークンを無効化**するべきです。この仕組みを「リフレッシュトークン・ローテーション」と呼びます。これにより、万が一リフレッシュトークンが漏洩しても、一度使われると無効になるため、継続的な不正利用を防ぐことができます。さらに、もし無効化されたはずの古いトークンが使用された場合、それはトークンが窃取された強力な兆候とみなし、そのユーザーに関連するすべてのトークンファミリーを即座に失効させるべきです 45。

#### **アプリケーションセッションの管理**

* **セッションの確立**: IDトークンを正常に検証した後、アプリケーションはユーザーのために独自のセッションを作成します 53。  
* **セッションタイムアウト**: このアプリケーションセッションには、独自のタイムアウトポリシーを設定する必要があります。例えば、Google Cloudでは、ユーザーのアクティビティに関わらず、デフォルトで16時間のセッション期間が設定されており、それを超えると再認証が要求されます 53。  
* **ログアウト処理**: ログアウトは、3つのレイヤーで処理する必要があります。  
  1. アプリケーションからのログアウト（自社のセッションCookieを無効化）。  
  2. サーバーサイドに保管しているリフレッシュトークンを失効させる。  
  3. （任意）ユーザーをGoogleのログアウトエンドポイントにリダイレクトさせ、Googleセッションからもログアウトさせる 56。

#### **Table 4: トークン保管メカニズムの比較（Webクライアント向け）**

開発者が安全な選択を行えるよう、Webクライアントにおけるトークンの保管方法をセキュリティの観点から比較します。

| メカニズム | 仕組み | 主な脆弱性 | 対策/ベストプラクティス |
| :---- | :---- | :---- | :---- |
| メモリ空間 | JavaScriptの変数として保持。 | ページリロードで消失。XSS攻撃に脆弱。 | リロードのたびに再認証が必要。短期的な利用に限定。推奨されない。 |
| localStorage / sessionStorage | ブラウザのキー・バリューストアに永続化。 | **XSS攻撃**。スクリプトからトークンを窃取可能。 | **使用を避けるべき**。現代のセキュリティ標準では非推奨 45。 |
| HttpOnly Cookie | HttpOnly属性付きCookieでトークンを保持。 | **CSRF攻撃**。JavaScriptからのアクセスは不可。 | SameSite属性（StrictまたはLax）とCSRFトークンを併用する必要がある 48。 |
| **BFF \+ サーバーサイドストレージ** | トークンはサーバー側で管理し、フロントとはHttpOnlyセッションCookieで通信。 | \- | **最も安全な現代的アプローチ**。トークンをブラウザから完全に隔離する 47。 |

### **3.3 システムへの統合：データベースとユーザー管理**

Google経由で認証されたユーザーを自社のシステムに統合する際には、将来的な拡張性とセキュリティを考慮したデータベース設計とユーザー管理戦略が不可欠です。

#### **黄金律：subをプライマリキーとして扱う**

* **sub (Subject) クレームの重要性**: IDトークンに含まれるsubクレームは、Googleユーザーを**一意かつ永続的に**識別するための唯一の保証された識別子です 15。  
* **メールアドレスを主キーにしてはならない**: ユーザーはGoogleアカウントのプライマリメールアドレスを変更することがあります。もしメールアドレスをデータベースの主キーとして使用していると、ユーザーがメールアドレスを変更した際に、システム上のアカウントとの紐付けが切れ、自分のアカウントにアクセスできなくなってしまいます。subクレームはメールアドレスが変更されても不変であるため、常にsubをユーザー識別のためのキーとして使用する必要があります 15。

#### **ユーザーデータベースのスキーマ設計**

柔軟で拡張性の高いユーザー管理システムを構築するためには、自社の内部ユーザーモデルと外部の認証プロバイダー情報を分離する設計が推奨されます。

* **usersテーブル**: アプリケーション固有のユーザー情報を格納します。id（例: UUID）、username、created\_atなどのカラムを持ちます。  
* **user\_identitiesテーブル**: 外部認証プロバイダーとの紐付けを管理するためのテーブルです 57。  
  * id (主キー)  
  * user\_id (usersテーブルへの外部キー)  
  * provider\_name (認証プロバイダー名、例: 'google')  
  * provider\_user\_id (プロバイダーから提供される一意のID、Googleの場合は\*\*subクレームの値\*\*)  
  * refresh\_token (暗号化して保存)  
  * access\_token (暗号化して保存)  
  * expires\_at (アクセストークンの有効期限)

この設計により、一人のユーザーがGoogle、パスワード、その他のソーシャルログインなど、複数の認証方法を単一の内部アカウントに紐付けることが可能になります。

#### **アカウントの連携とプロビジョニング**

このデータベース設計に基づいたユーザー処理のロジックは以下のようになります。

1. **ユーザーログイン**: ユーザーがGoogleでログインし、IDトークンがサーバーに渡されます。  
2. **subの確認**: サーバーはIDトークンからsubクレームを抽出し、user\_identitiesテーブル内にprovider\_name \= 'google'かつprovider\_user\_id \= {subの値}というレコードが存在するかを検索します。  
3. **既存ユーザーの場合**: レコードが見つかった場合、そのuser\_idに紐づくユーザーとしてログイン処理を行います。この際、IDトークンに含まれる新しいプロフィール情報（名前やプロフィール写真など）でusersテーブルの情報を更新することもできます。  
4. **新規ユーザーの場合**: レコードが見つからない場合、これは新規ユーザーです。  
   * まず、usersテーブルに新しいユーザーレコードを作成します。  
   * 次に、その新しいuser\_idと、IDトークンから得られたsub、プロバイダー名 ('google') を用いて、user\_identitiesテーブルに新しいレコードを作成します。  
   * これにより、新しいユーザーアカウントがプロビジョニングされ、Google IDと紐付けられます。  
5. **既存アカウントへの連携**: 既にパスワードで登録済みのユーザーが、後から「Googleでログイン」をアカウントに連携したい場合、ユーザーがログインしている状態でOAuthフローを開始させ、取得したsubをそのユーザーのuser\_idに紐付けてuser\_identitiesテーブルに新しいレコードを追加します。

このアプローチは、アプリケーションの認証ロジックとコアなビジネスロジックを明確に分離し、将来的に新たな認証プロバイダーを追加する際にも、システムへの影響を最小限に抑えることができる、非常に堅牢なアーキテクチャです 58。

## **第4部 結論と戦略的推奨事項**

本レポートでは、「Googleでログイン」機能の実装に必要なOAuth 2.0とOpenID Connectの技術的基盤から、具体的な実装手順、そして堅牢なシステムを構築するための高度なセキュリティプラクティスまでを詳細に解説しました。本章では、これまでの分析を総括し、導入のメリットとリスクを整理するとともに、実装チームが遵守すべき最終的なチェックリストを提示します。

### **4.1 メリットとリスクの総合分析**

「Googleでログイン」機能の導入は、多くの利点をもたらす一方で、慎重に管理すべきリスクも伴います。

#### **主なメリット**

* **ユーザー体験の向上**: 新規登録やログイン時のプロセスが大幅に簡素化されます。ユーザーは新しいパスワードを覚える必要がなく、ワンクリックでサービスを利用開始できるため、コンバージョン率の向上や離脱率の低下に直結します 4。  
* **セキュリティの強化**: パスワード管理という複雑でリスクの高い責務を、Googleのような信頼できるプロバイダーに委任できます。Googleは多要素認証（MFA）などの高度なセキュリティ対策をユーザーに提供しており、自社で同レベルのセキュリティを実装するコストとリスクを削減できます 4。  
* **開発の簡素化**: 認証システムの主要部分を自前で構築する必要がなくなるため、開発者はアプリケーションのコア機能の開発に集中できます 60。

#### **リスクと考慮事項**

* **実装の複雑性**: 導入が容易になる一方で、本レポートで詳述した通り、安全な実装にはOIDC、CSRF対策、PKCE、安全なトークン管理といった技術への深い理解が不可欠です。表面的な実装は深刻なセキュリティ脆弱性を生み出します 6。  
* **プロバイダーへの依存**: ログイン機能がGoogleのサービスの可用性に依存することになります。Googleのシステムに障害が発生した場合、ユーザーは貴社サービスにログインできなくなる可能性があります。  
* **データプライバシーの責務**: Googleから取得したユーザーデータ（名前、メールアドレス等）の管理者として、プライバシーポリシーを遵守し、適用される法規制（GDPR、APPIなど）に従ってデータを適切に取り扱う責任が生じます。  
* **サプライチェーンリスク**: ユーザーが他の悪意のあるアプリケーションにGoogleアカウントでのアクセスを許可してしまった場合、そのアカウント情報が危険に晒される可能性があります。直接的な攻撃ではなくとも、ユーザーのアカウントが侵害される間接的なリスクを考慮する必要があります 9。

### **4.2 最終実装チェックリストと推奨事項**

開発チームが本番リリース前に、実装のセキュリティと完全性を最終確認するためのチェックリストを以下に示します。これは、本レポートで解説した最も重要なセキュリティ制御を網羅しています。

* **\[ \] プロトコル**: 認証の基盤として、単なるOAuth 2.0ではなく、**OpenID Connect (OIDC)** を正しく使用しているか？  
* **\[ \] フロー**: すべてのクライアントタイプ（Web、モバイル、SPA）で、**PKCEを伴う認可コードグラントフロー**を使用しているか？  
* **\[ \] 通信の安全性**: アプリケーションのredirect\_uriを含め、すべての通信で**HTTPS**が強制されているか？  
* **\[ \] クライアント認証情報**: client\_secretはサーバーサイドで安全に保管され、決してクライアントに公開されていないか？  
* **\[ \] CSRF対策**: すべての認可リクエストにおいて、stateパラメータが生成・保存され、コールバック時に厳密に**検証**されているか？  
* **\[ \] リプレイ攻撃対策**: nonceパラメータが使用され、IDトークン内で返却された際に正しく**検証**されているか？  
* **\[ \] トークンの保管**: トークンはベストプラクティス（WebではBFF/サーバーサイドセッション、モバイルではOSのセキュアストレージ）に従って保管されているか？ localStorageは使用していないか？  
* **\[ \] トークンのライフサイクル**: 短命なアクセストークンと、**リフレッシュトークン・ローテーション**が実装されているか？  
* **\[ \] ユーザー識別**: データベース内でユーザーを識別するキーとして、メールアドレスではなく、不変である\*\*subクレーム\*\*を使用しているか？  
* **\[ \] スコープの最小化**: アプリケーションの機能に絶対的に必要な**最小限のスコープ**のみを要求しているか？  
* **\[ \] IDトークンの検証**: サーバーサイドで、IDトークンの**署名**、発行者 (iss)、対象者 (aud)、有効期限 (exp) を完全に検証しているか？  
* **\[ \] Googleによるアプリ審査**: センシティブスコープまたは制限付きスコープを使用している場合、Googleの**アプリ確認プロセス**を完了しているか？ 28

最終推奨事項:  
「Googleでログイン」は、現代のWebアプリケーションにとって強力なツールです。しかし、その恩恵を最大限に享受し、リスクを最小化するためには、本レポートで概説したセキュリティ原則とベストプラクティスを厳格に遵守することが絶対条件です。特に、BFFパターンの採用、PKCEとstateの必須利用、そしてリフレッシュトークン・ローテーションの実装は、現代のセキュリティ標準において妥協できない要素です。これらの原則に基づいた慎重な設計と実装こそが、ユーザーに安全で快適な体験を提供し、サービスの信頼性を確立する鍵となります。

#### **引用文献**

1. 図でざっくり解説 OAuth 2.0入門 \- Zenn, 8月 12, 2025にアクセス、 [https://zenn.dev/crebo\_tech/articles/article-0011-20241006](https://zenn.dev/crebo_tech/articles/article-0011-20241006)  
2. OAuth 2.0 とは何か、どのように役立つのか？ \- Auth0, 8月 12, 2025にアクセス、 [https://auth0.com/jp/intro-to-iam/what-is-oauth-2](https://auth0.com/jp/intro-to-iam/what-is-oauth-2)  
3. OAuth 2.0の認証と認可とは？初心者向けにわかりやすく解説 \- AQ Tech Blog, 8月 12, 2025にアクセス、 [https://techblog.asia-quest.jp/202410/what-is-oauth-2.0-authentication-and-authorization](https://techblog.asia-quest.jp/202410/what-is-oauth-2.0-authentication-and-authorization)  
4. OAuth 2.0について \#OAuth2.0 \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/3y9Mz/items/5ce8f48e5d852f8a0607](https://qiita.com/3y9Mz/items/5ce8f48e5d852f8a0607)  
5. OAuth 2.0 Scopes, 8月 12, 2025にアクセス、 [https://oauth.net/2/scope/](https://oauth.net/2/scope/)  
6. OAuth 2.0とは？仕組み、認証方式などを解説 \- Apidog, 8月 12, 2025にアクセス、 [https://apidog.com/jp/blog/oauth-2-guide/](https://apidog.com/jp/blog/oauth-2-guide/)  
7. Configure the OAuth consent screen and choose scopes | Google Workspace, 8月 12, 2025にアクセス、 [https://developers.google.com/workspace/guides/configure-oauth-consent](https://developers.google.com/workspace/guides/configure-oauth-consent)  
8. OpenID Connectの仕組みやOAuthとの違いを分かりやすく解説 ..., 8月 12, 2025にアクセス、 [https://udemy.benesse.co.jp/development/security/openid-connect.html](https://udemy.benesse.co.jp/development/security/openid-connect.html)  
9. SAMLとOAuthの違いとは？ それぞれのメリットや仕組みも解説 \- Gluegent, 8月 12, 2025にアクセス、 [https://www.gluegent.com/service/gate/column/saml-oauth/](https://www.gluegent.com/service/gate/column/saml-oauth/)  
10. OAuth (OAuth 2.0 認可フレームワーク) | 晴耕雨読, 8月 12, 2025にアクセス、 [https://tex2e.github.io/blog/protocol/oauth](https://tex2e.github.io/blog/protocol/oauth)  
11. OAuthとは？認証の仕組みと危険性 | Proofpoint JP, 8月 12, 2025にアクセス、 [https://www.proofpoint.com/jp/threat-reference/oauth](https://www.proofpoint.com/jp/threat-reference/oauth)  
12. Open ID Connectとは？概要とメリットを解説！ \- セキュリティ事業 \- マクニカ, 8月 12, 2025にアクセス、 [https://www.macnica.co.jp/business/security/manufacturers/okta/blog\_20210901.html](https://www.macnica.co.jp/business/security/manufacturers/okta/blog_20210901.html)  
13. 「Googleでログイン」の中身を見てみた（OpenID Connect） \- Zenn, 8月 12, 2025にアクセス、 [https://zenn.dev/kobababa/articles/88574c4372315f](https://zenn.dev/kobababa/articles/88574c4372315f)  
14. OpenID Connect とは何か、どのような目的に使うのか？ \- Auth0, 8月 12, 2025にアクセス、 [https://auth0.com/jp/intro-to-iam/what-is-openid-connect-oidc](https://auth0.com/jp/intro-to-iam/what-is-openid-connect-oidc)  
15. OpenID Connect | Sign in with Google | Google for Developers, 8月 12, 2025にアクセス、 [https://developers.google.com/identity/openid-connect/openid-connect?hl=ja](https://developers.google.com/identity/openid-connect/openid-connect?hl=ja)  
16. 最適な OAuth 2.0 フローの選び方 \- Authlete, 8月 12, 2025にアクセス、 [https://www.authlete.com/ja/kb/oauth-and-openid-connect/grant-type/how-to-choose-the-appropriate-oauth-2-flow/](https://www.authlete.com/ja/kb/oauth-and-openid-connect/grant-type/how-to-choose-the-appropriate-oauth-2-flow/)  
17. OAuth 2.0 認可コードフロー+PKCE をシーケンス図で理解する \- Zenn, 8月 12, 2025にアクセス、 [https://zenn.dev/zaki\_yama/articles/oauth2-authorization-code-grant-and-pkce](https://zenn.dev/zaki_yama/articles/oauth2-authorization-code-grant-and-pkce)  
18. OAuth 2.0 認可コードグラントフローについて理解したことをまとめます, 8月 12, 2025にアクセス、 [https://moneyforward-dev.jp/entry/2022/12/01/authorization-code-grant-flow/](https://moneyforward-dev.jp/entry/2022/12/01/authorization-code-grant-flow/)  
19. ウェブサーバー アプリケーションに OAuth 2.0 を使用する | Authorization, 8月 12, 2025にアクセス、 [https://developers.google.com/identity/protocols/oauth2/web-server?hl=ja](https://developers.google.com/identity/protocols/oauth2/web-server?hl=ja)  
20. Microsoft ID プラットフォームと OAuth 2.0 認証コード フロー, 8月 12, 2025にアクセス、 [https://learn.microsoft.com/ja-jp/entra/identity-platform/v2-oauth2-auth-code-flow](https://learn.microsoft.com/ja-jp/entra/identity-platform/v2-oauth2-auth-code-flow)  
21. Google API ConsoleでOAuthクライアントIDを作成する方法 \- Zenn, 8月 12, 2025にアクセス、 [https://zenn.dev/milky/articles/google-client-oauth](https://zenn.dev/milky/articles/google-client-oauth)  
22. クライアント ID の作成 | Cloud Endpoints Frameworks for App Engine, 8月 12, 2025にアクセス、 [https://cloud.google.com/endpoints/docs/frameworks/java/creating-client-ids?hl=ja](https://cloud.google.com/endpoints/docs/frameworks/java/creating-client-ids?hl=ja)  
23. Google Cloud Platform（GCP）の OAuth 認証の設定 \- Snowflake Documentation, 8月 12, 2025にアクセス、 [https://other-docs.snowflake.com/ja/connectors/google/gard/gard-connector-create-client-id](https://other-docs.snowflake.com/ja/connectors/google/gard/gard-connector-create-client-id)  
24. OAuth ウェブ クライアント ID を作成する \- Google Workspace Migrate, 8月 12, 2025にアクセス、 [https://support.google.com/workspacemigrate/answer/9222992?hl=ja](https://support.google.com/workspacemigrate/answer/9222992?hl=ja)  
25. Google OAuth認証クライアントの設定 \- サイボウズ製品 ヘルプ, 8月 12, 2025にアクセス、 [https://jp.cybozu.help/ja/settings/oauth/google.html](https://jp.cybozu.help/ja/settings/oauth/google.html)  
26. GCP(Google Cloud Platform)のプロジェクトでOAuth クライアントIDを作成する \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/genta-mz/items/c2dccb3a82141faac284](https://qiita.com/genta-mz/items/c2dccb3a82141faac284)  
27. Using OAuth 2.0 to Access Google APIs | Google Account Authorization, 8月 12, 2025にアクセス、 [https://developers.google.com/identity/protocols/oauth2](https://developers.google.com/identity/protocols/oauth2)  
28. OAuth App Verification Help Center \- Google Cloud Platform Console Help, 8月 12, 2025にアクセス、 [https://support.google.com/cloud/answer/13463073?hl=en](https://support.google.com/cloud/answer/13463073?hl=en)  
29. Manage App Data Access \- Google Cloud Platform Console Help, 8月 12, 2025にアクセス、 [https://support.google.com/cloud/answer/15549135?hl=en](https://support.google.com/cloud/answer/15549135?hl=en)  
30. OAuth 2.0のscopeを考える \#Rails \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/murs313/items/e4894b20f233995fd8d2](https://qiita.com/murs313/items/e4894b20f233995fd8d2)  
31. OAuth2認証が必要なGoogle Cloud APIをプログラムから実行する｜Daddy \- note, 8月 12, 2025にアクセス、 [https://note.com/daddysoffice/n/n8505a8ae8e98](https://note.com/daddysoffice/n/n8505a8ae8e98)  
32. Cloud Storage OAuth 2.0 scopes \- Google Cloud, 8月 12, 2025にアクセス、 [https://cloud.google.com/storage/docs/oauth-scopes](https://cloud.google.com/storage/docs/oauth-scopes)  
33. Google Scope Categories \- Cisco Umbrella Documentation, 8月 12, 2025にアクセス、 [https://docs.umbrella.com/cloudlock-documentation/docs/google-scope-categories](https://docs.umbrella.com/cloudlock-documentation/docs/google-scope-categories)  
34. Requesting Minimum Scopes \- Google Cloud Platform Console Help, 8月 12, 2025にアクセス、 [https://support.google.com/cloud/answer/13807380?hl=en](https://support.google.com/cloud/answer/13807380?hl=en)  
35. 【連載】世界一わかりみの深いOAuth入門 〜 その4:state ..., 8月 12, 2025にアクセス、 [https://tech-lab.sios.jp/archives/25642](https://tech-lab.sios.jp/archives/25642)  
36. OAuth2.0におけるstate(CSRF対策)とPKCEの違い \- Zenn, 8月 12, 2025にアクセス、 [https://zenn.dev/tell1124/articles/f6a4a12b3172ad](https://zenn.dev/tell1124/articles/f6a4a12b3172ad)  
37. OAuth2.0 State の役割 \#csrf \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/ist-n-m/items/67a5a0fb4f50ac1e30c1](https://qiita.com/ist-n-m/items/67a5a0fb4f50ac1e30c1)  
38. OAuth2.0におけるCSRFとは \- セキュアスカイプラス, 8月 12, 2025にアクセス、 [https://securesky-plus.com/engineerblog/1848/](https://securesky-plus.com/engineerblog/1848/)  
39. OAuth 2.0 認可コードグラント+PKCE 概要 \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/KWS\_0901/items/583a71c086302181fa29](https://qiita.com/KWS_0901/items/583a71c086302181fa29)  
40. PKCE とは：基本概念から深い理解へ \- Logto blog, 8月 12, 2025にアクセス、 [https://blog.logto.io/ja/how-pkce-protects-the-authorization-code-flow-for-native-apps](https://blog.logto.io/ja/how-pkce-protects-the-authorization-code-flow-for-native-apps)  
41. OAuth2.0 PKCEとは 〜Stateとの違い〜 \#認可 \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/ist-n-m/items/992c67b803ff460818ec](https://qiita.com/ist-n-m/items/992c67b803ff460818ec)  
42. モバイルアプリ認証の新常識 OAuth 2.0 PKCEフローの全貌と実装ガイド, 8月 12, 2025にアクセス、 [https://it-notes.stylemap.co.jp/webservice/%E3%83%A2%E3%83%90%E3%82%A4%E3%83%AB%E3%82%A2%E3%83%97%E3%83%AA%E8%AA%8D%E8%A8%BC%E3%81%AE%E6%96%B0%E5%B8%B8%E8%AD%98%E3%80%80oauth-2-0-pkce%E3%83%95%E3%83%AD%E3%83%BC%E3%81%AE%E5%85%A8%E8%B2%8C/](https://it-notes.stylemap.co.jp/webservice/%E3%83%A2%E3%83%90%E3%82%A4%E3%83%AB%E3%82%A2%E3%83%97%E3%83%AA%E8%AA%8D%E8%A8%BC%E3%81%AE%E6%96%B0%E5%B8%B8%E8%AD%98%E3%80%80oauth-2-0-pkce%E3%83%95%E3%83%AD%E3%83%BC%E3%81%AE%E5%85%A8%E8%B2%8C/)  
43. Auth0を利用してOAuth 2.0のPKCEを理解する \- DevelopersIO, 8月 12, 2025にアクセス、 [https://dev.classmethod.jp/articles/oauth-2-0-pkce-by-auth0/](https://dev.classmethod.jp/articles/oauth-2-0-pkce-by-auth0/)  
44. Proof Key for Code Exchange (PKCE) 拡張 \- Salesforce Help, 8月 12, 2025にアクセス、 [https://help.salesforce.com/s/articleView?id=sf.remoteaccess\_pkce.htm\&language=ja\&type=5](https://help.salesforce.com/s/articleView?id=sf.remoteaccess_pkce.htm&language=ja&type=5)  
45. Refresh Token Rotation: Best Practices for Developers \- Serverion, 8月 12, 2025にアクセス、 [https://www.serverion.com/uncategorized/refresh-token-rotation-best-practices-for-developers/](https://www.serverion.com/uncategorized/refresh-token-rotation-best-practices-for-developers/)  
46. Understanding Access and Refresh Tokens: A Beginner's Guide \- DEV Community, 8月 12, 2025にアクセス、 [https://dev.to/gervaisamoah/understanding-access-and-refresh-tokens-a-beginners-guide-4nn8](https://dev.to/gervaisamoah/understanding-access-and-refresh-tokens-a-beginners-guide-4nn8)  
47. いまさらLocal Storageとアクセストークンの保存場所の話について \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/NewGyu/items/0b3111b61405366a76c5](https://qiita.com/NewGyu/items/0b3111b61405366a76c5)  
48. Best practices for storing tokens \- CyberArk Docs, 8月 12, 2025にアクセス、 [https://docs.cyberark.com/identity/latest/en/content/developer/oidc/tokens/token-storage.htm](https://docs.cyberark.com/identity/latest/en/content/developer/oidc/tokens/token-storage.htm)  
49. Where should Access and Refresh tokens be stored? : r/dotnet \- Reddit, 8月 12, 2025にアクセス、 [https://www.reddit.com/r/dotnet/comments/1jfk2pm/where\_should\_access\_and\_refresh\_tokens\_be\_stored/](https://www.reddit.com/r/dotnet/comments/1jfk2pm/where_should_access_and_refresh_tokens_be_stored/)  
50. アクセストークンをどこに保存するか \#OAuth \- Qiita, 8月 12, 2025にアクセス、 [https://qiita.com/yngwie6120/items/8347459fa0f5c106e5c8](https://qiita.com/yngwie6120/items/8347459fa0f5c106e5c8)  
51. OAuth 2 Refresh Tokens: A Practical Guide \- Frontegg, 8月 12, 2025にアクセス、 [https://frontegg.com/blog/oauth-2-refresh-tokens](https://frontegg.com/blog/oauth-2-refresh-tokens)  
52. What Are Refresh Tokens and How to Use Them Securely \- Auth0, 8月 12, 2025にアクセス、 [https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/](https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/)  
53. Improve security posture with time bound session length | Google Cloud Blog, 8月 12, 2025にアクセス、 [https://cloud.google.com/blog/products/identity-security/improve-security-posture-with-time-bound-session-length](https://cloud.google.com/blog/products/identity-security/improve-security-posture-with-time-bound-session-length)  
54. asp.net web api \- Managing browser sessions with Google sign in \- Stack Overflow, 8月 12, 2025にアクセス、 [https://stackoverflow.com/questions/38319734/managing-browser-sessions-with-google-sign-in](https://stackoverflow.com/questions/38319734/managing-browser-sessions-with-google-sign-in)  
55. How to manage google sign-in session (Google Sign-In JavaScript client) \- Stack Overflow, 8月 12, 2025にアクセス、 [https://stackoverflow.com/questions/31074986/how-to-manage-google-sign-in-session-google-sign-in-javascript-client](https://stackoverflow.com/questions/31074986/how-to-manage-google-sign-in-session-google-sign-in-javascript-client)  
56. Best Practices for Application Session Management \- Auth0, 8月 12, 2025にアクセス、 [https://auth0.com/blog/application-session-management-best-practices/](https://auth0.com/blog/application-session-management-best-practices/)  
57. 生成 AI アプリがアクセスするデータベースにユーザー認証を実装する | Google Cloud 公式ブログ, 8月 12, 2025にアクセス、 [https://cloud.google.com/blog/ja/products/ai-machine-learning/build-user-authentication-into-your-genai-app-accessing-database](https://cloud.google.com/blog/ja/products/ai-machine-learning/build-user-authentication-into-your-genai-app-accessing-database)  
58. Account authentication and password management best practices | Google Cloud Blog, 8月 12, 2025にアクセス、 [https://cloud.google.com/blog/products/identity-security/account-authentication-and-password-management-best-practices](https://cloud.google.com/blog/products/identity-security/account-authentication-and-password-management-best-practices)  
59. 最近よく見る「Googleでログイン」 の仕組みやメリットを解説, 8月 12, 2025にアクセス、 [https://login-plus.jp/posts/Sign-in-with-Google](https://login-plus.jp/posts/Sign-in-with-Google)  
60. OAuth、OpenID Connectとは？ 仕組みや違いについて解説 \- Gluegent, 8月 12, 2025にアクセス、 [https://www.gluegent.com/service/gate/column/oauthopenid\_connect/](https://www.gluegent.com/service/gate/column/oauthopenid_connect/)  
61. Setting up OAuth 2.0 \- API Console Help \- Google Help, 8月 12, 2025にアクセス、 [https://support.google.com/googleapi/answer/6158849?hl=en](https://support.google.com/googleapi/answer/6158849?hl=en)